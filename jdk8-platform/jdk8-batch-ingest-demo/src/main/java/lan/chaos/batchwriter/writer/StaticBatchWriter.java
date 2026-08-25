package lan.chaos.batchwriter.writer;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 对照方案：固定批量 + Pipeline。批量大小在构造时定死（不随负载变化）。
 *
 * <p>用于对照 {@code adaptive}：证明"批量收益真实存在（static 优于 legacy）"
 * 以及"固定批量存在最优值只有自适应能逼近"。
 */
public class StaticBatchWriter implements BatchWriter<String> {

    private final StringRedisTemplate redis;
    private final String key;
    private final int batchSize;
    private final ArrayBlockingQueue<String> queue;
    private final long idleFlushNs;

    private final AtomicLong written = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();

    private volatile boolean running;
    private Thread consumer;

    public StaticBatchWriter(StringRedisTemplate redis, String key, int batchSize, int queueCapacity, long idleFlushNs) {
        this.redis = redis;
        this.key = key;
        this.batchSize = batchSize;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.idleFlushNs = idleFlushNs;
    }

    @Override
    public String name() {
        return "static";
    }

    @Override
    public void write(String item) {
        // 队列满则丢弃计数（最小版本：不阻塞汇聚端）
        if (!queue.offer(item)) {
            dropped.incrementAndGet();
        }
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

    @Override
    public void start() {
        running = true;
        consumer = new Thread(this::drainLoop, "static-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    @Override
    public void close() {
        running = false;
        // 刷出剩余
        flushTail();
    }

    private void drainLoop() {
        ArrayList<String> batch = new ArrayList<>(batchSize);
        long lastItemTs = System.nanoTime();
        while (running) {
            // 尽量快速攒满一个目标批量（来自队列已有积压的用 drainTo 瞬取，空则 poll 等待）
            if (batch.size() < batchSize) {
                // 仅按"实际新抽取条数"刷新空闲计时，防止残留不满批 + 空队列时永远不触发 idle
                int n = queue.drainTo(batch, batchSize - batch.size());
                if (n > 0) {
                    lastItemTs = System.nanoTime();
                } else {
                    String item;
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
            boolean full = batch.size() >= batchSize;
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
            Thread.sleep(0, 300_000); // 0.3ms，让汇聚端有机会补货，攒成更大批次
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public long errors() {
        return errors.get();
    }

    public long dropped() {
        return dropped.get();
    }

    /** 存储钩子：写一批，返回是否成功（Redis 实现 = 一次 Pipeline；测试可覆写为内存计数）。 */
    protected boolean storage(List<String> batch) {
        RedisBatchSupport.pipelineHash(redis, key, batch);
        return true;
    }

    private void flush(ArrayList<String> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            if (storage(batch)) {
                written.addAndGet(batch.size());
                batches.incrementAndGet();
            } else {
                errors.addAndGet(batch.size());
            }
        } catch (RuntimeException e) {
            errors.addAndGet(batch.size());
        }
        batch.clear();
    }

    private void flushTail() {
        ArrayList<String> rest = new ArrayList<>(queue.size());
        queue.drainTo(rest);
        if (!rest.isEmpty()) {
            try {
                if (storage(rest)) {
                    written.addAndGet(rest.size());
                    batches.incrementAndGet();
                } else {
                    errors.addAndGet(rest.size());
                }
            } catch (RuntimeException e) {
                errors.addAndGet(rest.size());
            }
        }
    }
}