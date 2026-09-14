package lan.chaos.leaderboard.pipeline;

import lan.chaos.leaderboard.common.model.RankEntry;
import lan.chaos.leaderboard.common.store.ScoreBoard;
import lan.chaos.leaderboard.common.store.ScoreBoardFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ⑤ 异步更新流水线（对应面经「扩展方案 - 异步更新流水线（Kafka 解耦）」）。
 *
 * <p>高频积分更新先投递到内存有界队列，后台单线程<b>攒批</b>后批量写入 ScoreBoard，
 * 把「多次随机写」合并为「少量批量写」，大幅降低存储侧压力（生产中用 Kafka 做同样的削峰解耦）。
 * 提供 {@link #submit}、{@link #awaitQuiescent} 便于测试断言。</p>
 */
@Service
public class AsyncScorePipeline implements DisposableBean {

    /** 一次投递单元 */
    private static final class ScoreDelta {
        final String userId;
        final double delta;

        ScoreDelta(String userId, double delta) {
            this.userId = userId;
            this.delta = delta;
        }
    }

    private final ScoreBoard board;
    private final int batchSize;
    private final long flushMs;
    private final BlockingQueue<ScoreDelta> queue = new ArrayBlockingQueue<>(1 << 16);

    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong flushed = new AtomicLong();
    private final AtomicLong batches = new AtomicLong();
    private final AtomicInteger inflight = new AtomicInteger();

    private volatile boolean running = true;
    private final Thread worker;

    @Autowired
    public AsyncScorePipeline(ScoreBoardFactory factory,
                               @Value("${leaderboard.pipeline-batch:500}") int batchSize,
                               @Value("${leaderboard.pipeline-flush-ms:50}") long flushMs) {
        this.board = factory.create("pipeline");
        this.batchSize = batchSize;
        this.flushMs = flushMs;
        this.worker = new Thread(this::loop, "async-score-worker");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /** 投递一次积分变更（非阻塞，入队即返回） */
    public void submit(String userId, double delta) {
        submitted.incrementAndGet();
        try {
            queue.put(new ScoreDelta(userId, delta));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 后台循环：攒满一批或超时即批量落盘 */
    private void loop() {
        List<ScoreDelta> buf = new ArrayList<>(batchSize);
        while (running || !queue.isEmpty()) {
            buf.clear();
            int drained = queue.drainTo(buf, batchSize);
            if (drained == 0) {
                if (!running) {
                    break;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(flushMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            flush(buf);
        }
    }

    private void flush(List<ScoreDelta> buf) {
        inflight.addAndGet(buf.size());
        try {
            for (ScoreDelta d : buf) {
                board.incrementScore(d.userId, d.delta);
            }
            flushed.addAndGet(buf.size());
            batches.incrementAndGet();
        } finally {
            inflight.addAndGet(-buf.size());
        }
    }

    /** 同步强制刷盘（测试 / 关闭前兜底） */
    public void flushNow() {
        List<ScoreDelta> buf = new ArrayList<>(batchSize);
        while (queue.drainTo(buf, batchSize) > 0) {
            flush(buf);
            buf.clear();
        }
    }

    /** 等待队列清空且无在途批次（测试断言用） */
    public boolean awaitQuiescent(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (queue.isEmpty() && inflight.get() == 0) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(5);
        }
        return queue.isEmpty() && inflight.get() == 0;
    }

    public List<RankEntry> topN(int n) {
        return board.topN(n);
    }

    public String stats() {
        return String.format("submitted=%d, flushed=%d, batches=%d, queue=%d",
                submitted.get(), flushed.get(), batches.get(), queue.size());
    }

    public String run() {
        board.clear();
        // 模拟 1000 次高频更新：100 个用户各自 +10 分
        for (int r = 0; r < 10; r++) {
            for (int i = 1; i <= 100; i++) {
                submit("u" + i, 1);
            }
        }
        StringBuilder sb = new StringBuilder();
        try {
            awaitQuiescent(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sb.append("异步流水线跑完 -> ").append(stats()).append("\n");
        sb.append("u1 最终积分=").append(board.getScore("u1")).append("（应为 10.0）").append("\n");
        sb.append("topN(3): ").append(topN(3)).append("\n");
        shutdown();
        return sb.toString();
    }

    /** 关闭后台线程（DisposableBean：Spring 容器关闭时自动调用） */
    public void shutdown() {
        running = false;
        worker.interrupt();
        try {
            worker.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void destroy() {
        shutdown();
    }
}
