package lan.chaos.batchwriter.writer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定批引擎纯内存验证（无需 Redis）：满量汇聚下应攒到目标批量、无丢失。
 */
class StaticBatchWriterTest {

    /** 内存 storage 覆写，去掉 Redis 依赖，仅验证攒批与不漏。 */
    private static final class MemWriter extends StaticBatchWriter {

        final AtomicLong count = new AtomicLong();

        MemWriter(int batchSize, int capacity) {
            super(null, "k", batchSize, capacity, 2000);
        }

        @Override
        protected boolean storage(List<String> batch) {
            count.addAndGet(batch.size());
            return true;
        }
    }

    @Test
    void batchesAndDrainsAll() {
        MemWriter w = new MemWriter(1024, 200_000);
        w.start();
        int total = 100_000;
        for (int i = 0; i < total; i++) {
            w.write("x" + i);
        }
        long deadline = System.currentTimeMillis() + 5000;
        while (w.count.get() < total && System.currentTimeMillis() < deadline) {
            Thread.yield();
        }
        w.close();

        assertEquals(total, w.count.get(), "全部写入，无丢失");
        assertTrue(w.avgBatchSize() >= 512, "定批应接近目标 1024，实际=" + w.avgBatchSize());
    }
}