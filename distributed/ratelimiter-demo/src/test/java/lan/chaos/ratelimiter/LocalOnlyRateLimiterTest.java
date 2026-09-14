package lan.chaos.ratelimiter;

import lan.chaos.ratelimiter.local.LocalOnlyRateLimiter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 纯本地限流：单实例可按自身配额放行，超配额拒绝。
 * 强调：多实例下各自满额会按节点数成倍超限（这正是 local-redis 要解决的）。
 */
class LocalOnlyRateLimiterTest {

    @Test
    void singleInstanceLimits() {
        LocalOnlyRateLimiter l = new LocalOnlyRateLimiter(10, 10); // 10/s
        for (int i = 0; i < 10; i++) {
            assertTrue(l.tryAcquire("tenant-a"), "容量内应放行 i=" + i);
        }
        assertFalse(l.tryAcquire("tenant-a"), "瞬时已耗尽应拒绝");
    }
}