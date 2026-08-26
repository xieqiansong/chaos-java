package lan.chaos.virtualthread;

import lan.chaos.virtualthread.common.model.LoadResult;
import lan.chaos.virtualthread.pinning.PinningCompare;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * pinning 断言：synchronized 临界区内阻塞 → 峰值并发被限制在载体线程数内、总耗时显著变长；
 * ReentrantLock 版本正常卸载，峰值并发可远超载体线程数。
 */
class PinningCompareTest {

    @Test
    void synchronizedBlocking_pinsCarrier_whileLockBlockingDoesNot() {
        PinningCompare.Comparison c = new PinningCompare(200, 10).run();
        LoadResult pinned = c.pinned();
        LoadResult unlocked = c.unlocked();

        assertTrue(pinned.getCostMillis() > unlocked.getCostMillis() * 2.0,
                () -> "pinned 版耗时(" + pinned.getCostMillis() + "ms)应显著大于 unlocked 版(" + unlocked.getCostMillis() + "ms)");
        assertTrue(pinned.getPeakConcurrency() < unlocked.getPeakConcurrency(),
                () -> "pinned 版峰值并发(" + pinned.getPeakConcurrency() + ")应小于 unlocked 版(" + unlocked.getPeakConcurrency() + ")");
        assertTrue(pinned.getPeakConcurrency() <= c.carriers(),
                () -> "pinned 版峰值并发(" + pinned.getPeakConcurrency() + ")应不超过载体线程数(" + c.carriers() + ")");
    }
}
