package lan.chaos.virtualthread;

import lan.chaos.virtualthread.common.model.LoadResult;
import lan.chaos.virtualthread.throughput.ThroughputCompare;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 吞吐对比断言：虚拟线程组耗时应显著小于平台线程池组（IO 阻塞让出调度器）。
 */
class ThroughputCompareTest {

    @Test
    void virtualThreads_areFasterThanFixedPool_forIoBoundLoad() {
        ThroughputCompare compare = new ThroughputCompare(500, 5, 8);
        LoadResult fixed = compare.runOnFixedPool();
        LoadResult vt = compare.runOnVirtualThreads();

        assertTrue(vt.getCostMillis() < fixed.getCostMillis() / 2.0,
                () -> "虚拟线程组(" + vt.getCostMillis() + "ms)应明显快于平台线程组(" + fixed.getCostMillis() + "ms)");
        assertTrue(fixed.getPeakConcurrency() <= 8, "平台线程池峰值并发应不超过线程数");
    }
}
