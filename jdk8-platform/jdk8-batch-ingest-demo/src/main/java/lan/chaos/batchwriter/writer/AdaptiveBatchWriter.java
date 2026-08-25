package lan.chaos.batchwriter.writer;

import lan.chaos.batchwriter.config.BatchWriterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 核心：自适应批量入库引擎。
 *
 * <p>解决固定批量/逐条写入的两个问题：
 * <ol>
 *   <li>命令数随条目数线性放大（逐条）——用「内存桶攒批 + 一次 Pipeline」把命令数降到批次数；</li>
 *   <li>最优批量大小随负载/网络/存储状态动态变化（固定批）——用「探索-反馈-平滑」在线寻优。</li>
 * </ol>
 *
 * <p>机制（去敏后的通用思路）：
 * <ul>
 *   <li><b>一级内存桶</b>：有界 {@link ArrayBlockingQueue}，单消费线程 poll 攒批，给存储天然削峰；</li>
 *   <li><b>水位触发</b>：队列仍"重"(size≥当前批量) → 连打；攒够目标 → flush；空转超时 → 兜底 flush；</li>
 *   <li><b>加速线程</b>：队列超 {@code queueCritical} 时启用第二消费线程并发削峰，降到临界下退出；</li>
 *   <li><b>批量自适应</b>（本类核心）：候选 = 当前批量×{0.5,0.8,1.0,1.25,2.0}；
 *       探索期以 {code exploreProb} 概率试探候选做性能采样；反馈用<b>指数衰减加权平均</b>得到各候选吞吐；
 *       每 {code sampleWindow} 次 flush 挑「加权最优」并以平滑系数过渡，收敛稳定。</li>
 *   <li><b>监控</b>：200 样本窗口统计 TPS / 批量耗时 / 队列水位，周期性日志。</li>
 * </ul>
 *
 * <p>子类仅需实现 {@link #storage(List)} 返回该批次耗时（纳秒），即可插接到任意存储。
 */
public abstract class AdaptiveBatchWriter<T> implements BatchWriter<T> {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveBatchWriter.class);

    /** 候选批量倍数（含 1.0 = 保持当前，供寻优选择） */
    private static final double[] MULTIPLIERS = {0.5, 0.8, 1.0, 1.25, 2.0};

    // ---------- 配置 ----------
    protected final int queueCapacity;
    protected final int queueCritical;
    protected final int batchMin;
    protected final int batchMax;
    protected final double exploreProb;
    protected final long idleFlushNs;
    protected final double decayFactor;
    protected final double smoothCurrentWeight;
    protected final int sampleWindow;
    protected final boolean useAccelerator;

    // ---------- 运行时 ----------
    protected final ArrayBlockingQueue<T> queue;
    private volatile int candidateBatchSize;
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private long flushCount;
    /** 候选批量 → 速度样本（指数衰减加权平均） */
    private final Map<Integer, Speed> speeds = new ConcurrentHashMap<>();

    private volatile boolean running;
    private Thread consumer;
    private Thread accelerator;

    protected AdaptiveBatchWriter(BatchWriterProperties p) {
        this.queueCapacity = p.getQueueCapacity();
        this.queueCritical = p.getQueueCritical();
        this.batchMin = p.getBatchMin();
        this.batchMax = p.getBatchMax();
        this.exploreProb = p.getExploreProb();
        this.idleFlushNs = p.getIdleFlushMs() * 1_000_000L;
        this.decayFactor = p.getDecayFactor();
        this.smoothCurrentWeight = p.getSmoothCurrentWeight();
        this.sampleWindow = p.getSampleWindow();
        this.useAccelerator = p.isUseAccelerator();
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.candidateBatchSize = p.getBatchInitial();
    }

    /** 把一批 item 写入存储，返回该批次耗时（纳秒）。 */
    protected abstract long storage(List<T> batch);

    @Override
    public void write(T item) {
        if (!queue.offer(item)) {
            dropped.incrementAndGet();
        }
    }

    @Override
    public String name() {
        return "adaptive";
    }

    @Override
    public long itemsWritten() {
        return written.get();
    }

    @Override
    public long redisCalls() {
        return batches.get();
    }

    @Override
    public double avgBatchSize() {
        long b = batches.get();
        return b == 0 ? 0 : (double) written.get() / b;
    }

    /** 当前寻优中的目标批量（供监控/外部观察） */
    public int currentBatchSize() {
        return candidateBatchSize;
    }

    public long dropped() {
        return dropped.get();
    }

    @Override
    public long errors() {
        return errors.get();
    }

    @Override
    public void start() {
        running = true;
        consumer = new Thread(this::drainLoop, "adaptive-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    @Override
    public void close() {
        running = false;
        if (accelerator != null) {
            accelerator.interrupt();
        }
        flushTail();
    }

    // ================= 生产循环 =================

    private void drainLoop() {
        ArrayList<T> batch = new ArrayList<>(Math.min(candidateBatchSize * 2, 1 << 16));
        long lastItemTs = System.nanoTime();
        while (running) {
            // 水位 → 加速线程启停
            toggleAcceleratorLocked();

            // 快速攒满一个目标批量：先用 drainTo 瞬取已有积压，空则 poll 等待
            int target = candidateBatchSize;
            if (batch.size() < target) {
                // 仅按"实际新抽取条数"刷新空闲计时；
                // 残留不满批 + 空队列时必须走 idle 兜底，否则 lastItemTs 被持续刷新导致永远不刷
                int n = queue.drainTo(batch, target - batch.size());
                if (n > 0) {
                    lastItemTs = System.nanoTime();
                } else {
                    T item;
                    try {
                        item = queue.poll(1, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (item != null) {
                        batch.add(item);
                        lastItemTs = System.nanoTime();
                    }
                }
            }
            if (batch.isEmpty()) {
                continue;
            }
            boolean full = batch.size() >= target;
            boolean idle = System.nanoTime() - lastItemTs >= idleFlushNs;
            if (full || idle) {
                flush(batch);
            } else {
                pause();
            }
        }
        // 关闭时刷出消费线程残批（running=false 退出 while 后）
        if (!batch.isEmpty()) {
            flush(batch);
        }
    }

    private static void pause() {
        try {
            Thread.sleep(0, 300_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 加速线程启停：队列超出临界水位时启动第二消费线程，降到水位（临界的一半）下停用。
     * 锁保证只有一个加速线程生命周期处于同步状态。
     */
    private synchronized void toggleAcceleratorLocked() {
        boolean over = queue.size() > queueCritical;
        if (useAccelerator && over && accelerator == null) {
            Thread t = new Thread(this::acceleratorLoop, "adaptive-accelerator");
            t.setDaemon(true);
            accelerator = t;
            t.start();
            log.info("accelerator started, queue.size={}", queue.size());
        } else if (!over && accelerator != null) {
            Thread t = accelerator;
            accelerator = null;
            t.interrupt();
            log.info("accelerator stopped, queue.size={}", queue.size());
        }
    }

    /** 加速消费线程：不加注册反馈地快速排空队列（只更新 written/batches 计数）。 */
    private void acceleratorLoop() {
        ArrayList<T> tmp = new ArrayList<>(1024);
        while (running && accelerator == Thread.currentThread()) {
            // 降到临界一半以下即退
            if (queue.size() <= queueCritical / 2) {
                break;
            }
            T item = queue.poll();
            if (item == null) {
                // 可能被并发 drain，短暂退避
                try {
                    Thread.sleep(0, 500_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            tmp.add(item);
            if (tmp.size() >= 1024) {
                tryFlush(tmp, true);
            }
        }
        if (!tmp.isEmpty()) {
            queue.drainTo(tmp);
            tryFlush(tmp, false);
        }
    }

    /** 加速线程排空写：Redis 抖动失败时计入 errors 并丢弃该批。 */
    private void tryFlush(ArrayList<T> batch, boolean accelerated) {
        if (batch.isEmpty()) {
            return;
        }
        int n = batch.size();
        try {
            storage(batch);
            written.addAndGet(n);
            batches.incrementAndGet();
        } catch (RuntimeException e) {
            errors.addAndGet(n);
        }
        batch.clear();
    }

    // ================= flush 与自适应寻优 =================

    private void flush(ArrayList<T> batch) {
        if (batch.isEmpty()) {
            return;
        }
        int declared = candidateBatchSize;
        // 探索：一定概率试探候选批量（用 multiply 上的值作为反馈桶）
        if (ThreadLocalRandom.current().nextDouble() < exploreProb) {
            int c = pickRandomCandidate();
            if (c > 0) {
                declared = c;
            }
        }
        long el;
        try {
            el = storage(batch);
            written.addAndGet(batch.size());
            batches.incrementAndGet();
        } catch (RuntimeException e) {
            errors.addAndGet(batch.size());
            batch.clear();
            return;
        }

        recordSpeed(declared, batch.size(), el);
        batch.clear();

        flushCount++;
        if (flushCount % sampleWindow == 0) {
            adjustBatchSize();
        }
    }

    private int pickRandomCandidate() {
        double[] ms = MULTIPLIERS;
        double m = ms[ThreadLocalRandom.current().nextInt(ms.length)];
        return clamp((int) (candidateBatchSize * m));
    }

    private void recordSpeed(int bucket, int count, long elNs) {
        speeds.computeIfAbsent(bucket, k -> new Speed()).update(count, elNs, System.nanoTime());
    }

    /** 每 sampleWindow 次 flush：从候选里挑「加权吞吐最优」并平滑过渡。 */
    private void adjustBatchSize() {
        int cur = candidateBatchSize;
        int best = cur;
        double bestSpeed = -1;
        boolean any = false;
        for (double m : MULTIPLIERS) {
            int c = clamp((int) (cur * m));
            Speed s = speeds.get(c);
            if (s != null && s.samples > 0 && s.speed > bestSpeed) {
                bestSpeed = s.speed;
                best = c;
                any = true;
            }
        }
        if (any) {
            candidateBatchSize = clamp((int) (cur * smoothCurrentWeight + best * (1 - smoothCurrentWeight)));
            log.info("adaptive adjust: {} -> {} (best={}, speed={}/s)", cur, candidateBatchSize, best, (int) bestSpeed);
        }
    }

    int clamp(int v) {
        return Math.max(batchMin, Math.min(batchMax, v));
    }

    private void flushTail() {
        ArrayList<T> rest = new ArrayList<>(Math.max(1, queue.size()));
        queue.drainTo(rest);
        if (!rest.isEmpty()) {
            try {
                storage(rest);
                written.addAndGet(rest.size());
                batches.incrementAndGet();
            } catch (RuntimeException e) {
                errors.addAndGet(rest.size());
            }
        }
    }

    /** 单个候选批量的指数衰减加权平均速度（条/秒）。 */
    private final class Speed {
        int samples;
        double speed;
        double weight;
        long lastTs;

        void update(int count, long elNs, long now) {
            double inst = count * 1_000_000_000.0 / Math.max(elNs, 1);
            double dt = (lastTs == 0) ? 0 : Math.max(0, (now - lastTs) / 1_000_000_000.0);
            lastTs = now;
            if (samples == 0) {
                speed = inst;
                weight = 1;
            } else {
                double decay = dt <= 0 ? 1 : Math.pow(decayFactor, dt);
                double prevW = weight * decay;
                speed = (prevW * speed + inst) / (prevW + 1);
                weight = prevW + 1;
            }
            samples++;
        }
    }
}