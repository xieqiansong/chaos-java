package lan.chaos.virtualthread.bench;

import lan.chaos.virtualthread.common.constant.ExecutorMode;
import lan.chaos.virtualthread.common.model.BenchResult;
import lan.chaos.virtualthread.common.util.IoSimulator;
import lan.chaos.virtualthread.common.util.LatencyRecorder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 压测引擎冒烟：用极小参数验证「引擎本身是对的」，秒级完成，随日常 mvn test 一起跑。
 * WHY 单独拆出来：完整压测要跑满五个场景（分钟级），不该拖慢日常测试；
 * 但引擎若算错了吞吐或漏了任务，报告里所有数字都是废的——所以引擎必须有一道常驻防线。
 */
class BenchSanityTest {

    @Test
    void engine_completesAllTasksWithoutRejection() {
        BenchOptions options = BenchOptions.builder()
                .taskCount(200).concurrency(50).platformThreads(16)
                .queueCapacity(10_000).ioMillis(2).warmupRounds(0).build();

        BenchResult result = BenchEngine.run("冒烟", ExecutorMode.PLATFORM, options,
                index -> IoSimulator.ioBlock(2));

        assertEquals(200, result.success(), "所有任务都应成功");
        assertEquals(0, result.failed(), "不应有失败任务");
        assertEquals(0, result.rejected(), "队列充足时不应有拒绝");
        assertTrue(result.throughputPerSec() > 0, "吞吐应为正数");
    }

    @Test
    void virtualThreads_outrunSmallPool_evenAtTinyScale() {
        // 池只有 4 个线程、并发 200：平台池必须排队，虚拟线程直接铺开
        BenchOptions options = BenchOptions.builder()
                .taskCount(200).concurrency(200).platformThreads(4)
                .ioMillis(10).warmupRounds(0).build();
        BenchEngine.BenchTask task = index -> IoSimulator.ioBlock(10);

        BenchResult platform = BenchEngine.run("平台", ExecutorMode.PLATFORM, options, task);
        BenchResult virtual = BenchEngine.run("虚拟", ExecutorMode.VIRTUAL, options, task);

        assertTrue(virtual.throughputPerSec() > platform.throughputPerSec() * 2,
                () -> "极小规模下虚拟线程吞吐(" + String.format("%.0f", virtual.throughputPerSec())
                        + ")也应显著高于 4 线程池(" + String.format("%.0f", platform.throughputPerSec()) + ")");
        assertEquals(200, virtual.success());
        assertEquals(0, virtual.rejected());
    }

    @Test
    void latencyRecorder_calculatesPercentiles() {
        LatencyRecorder recorder = new LatencyRecorder(100);
        for (int i = 1; i <= 100; i++) {
            recorder.record(i * 1_000_000L); // 1ms ~ 100ms
        }
        assertEquals(100, recorder.count());
        assertEquals(50D, recorder.p50(), 0.01, "p50 应为 50ms");
        assertEquals(99D, recorder.p99(), 0.01, "p99 应为 99ms");
        assertEquals(50.5D, recorder.avgMillis(), 0.01, "平均值应为 50.5ms");
        assertEquals(100L, recorder.maxMillis());
    }
}
