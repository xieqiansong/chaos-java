package lan.chaos.ratelimiter.local;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 令牌桶核心语义测试（纯内存，零外部依赖，直接跑）。
 */
class LocalBucketTest {

    @Test
    void rejectsWhenUninitialized() {
        LocalBucket b = new LocalBucket();
        assertFalse(b.tryAcquire());
    }

    @Test
    void allowsFullCapacityAtOnce() {
        LocalBucket b = new LocalBucket();
        long now = System.nanoTime();
        b.init(100, 100, now);
        for (int i = 0; i < 100; i++) {
            assertTrue(b.tryAcquire(), "容量内应放行 i=" + i);
        }
        assertFalse(b.tryAcquire(), "超出容量应拒绝");
    }

    @Test
    void refillsAtRate() throws InterruptedException {
        LocalBucket b = new LocalBucket();
        b.init(1000, 1000, System.nanoTime());   // 1000/s，容量 1000
        while (b.tryAcquire()) {                  // 先耗尽初始容量
        }
        Thread.sleep(15);                          // 1000/s → 15ms ≈ 15 个令牌
        int got = 0;
        long deadline = System.nanoTime() + 500_000_000L; // 兜底防超时
        while (b.tryAcquire()) {
            got++;
            if (System.nanoTime() > deadline) {
                break;
            }
        }
        assertTrue(got >= 10 && got <= 20, "15ms 应补约 15 个令牌，实际 " + got);
    }

    @Test
    void calibrateShrinksAndDropsExcessTokens() {
        LocalBucket b = new LocalBucket();
        long now = System.nanoTime();
        b.init(100, 100, now);
        b.calibrate(100, 5, now);
        int got = 0;
        while (b.tryAcquire()) {
            got++;
        }
        assertEquals(5, got, "容量收缩后最多 5 个");
    }

    @Test
    void servedCounterResets() {
        LocalBucket b = new LocalBucket();
        long now = System.nanoTime();
        b.init(100, 100, now);
        for (int i = 0; i < 100; i++) {
            b.tryAcquire();
        }
        assertEquals(100, b.takeAndResetServed());
        assertEquals(0, b.takeAndResetServed());
    }
}