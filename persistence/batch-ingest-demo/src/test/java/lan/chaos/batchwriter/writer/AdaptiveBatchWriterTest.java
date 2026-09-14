package lan.chaos.batchwriter.writer;

import lan.chaos.batchwriter.config.BatchWriterProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自适应引擎纯内存验证（无需 Redis）：
 * 满量汇聚时所有条目都应被写走、平均批量应明显大于 1、批量大小能收敛到上界。
 */
class AdaptiveBatchWriterTest {

    /** 内存版 storage：只计数，模拟存储耗时（0），去掉网络作为纯 CPU 汇聚上界。 */
    private static final class MemWriter extends AdaptiveBatchWriter<String> {
        final AtomicLong stored = new AtomicLong();

        MemWriter(BatchWriterProperties p) {
            super(p);
        }

        @Override
        protected long storage(List<String> batch) {
            stored.addAndGet(batch.size());
            return 0;
        }
    }

    @Test
    void adaptsAndDrainsAll() {
        BatchWriterProperties p = new BatchWriterProperties();
        p.setQueueCapacity(600_000);
        p.setBatchInitial(1024);
        p.setBatchMin(16);
        p.setBatchMax(4096);
        p.setSampleWindow(20);
        p.setIdleFlushMs(200); // 兜底微短，便于测试窗口内刷出不满批的尾部

        MemWriter w = new MemWriter(p);
        w.start();
        int total = 300_000;
        for (int i = 0; i < total; i++) {
            // 队列容量(600k) > 总条数(300k)，即使消费线程吃紧也不至于满，故 offer 必成功
            assertTrue(w.queue.offer("x" + i), "队列不应在汇聚阶段满");
        }
        // 等待消费线程排空 + 兜底 flush
        long deadline = System.currentTimeMillis() + 5000;
        while (w.stored.get() < total && System.currentTimeMillis() < deadline) {
            Thread.yield();
        }
        w.close();

        assertEquals(total, w.stored.get(), "所有条目都应被写走，无丢失");
        assertTrue(w.avgBatchSize() > 1, "平均批量应 > 1，实际=" + w.avgBatchSize());
        assertTrue(w.redisCalls() > 0);
        assertTrue(drainedAll(w, total), "writer.itemsWritten() 应等于总条目数");
    }

    @Test
    void batchSizeClampsWithinBounds() {
        BatchWriterProperties p = new BatchWriterProperties();
        p.setBatchMin(64);
        p.setBatchMax(512);
        p.setBatchInitial(128);
        MemWriter w = new MemWriter(p);
        // 白盒：极端候选 clamp 到 [min,max]
        assertTrue(w.clamp(9999) <= 512);
        assertTrue(w.clamp(1) >= 64);
    }

    private static boolean drainedAll(MemWriter w, int total) {
        return w.itemsWritten() == total;
    }
}