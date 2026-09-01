package lan.chaos.virtualthread.bench.scenario;

import lan.chaos.virtualthread.bench.BenchEngine;
import lan.chaos.virtualthread.bench.BenchOptions;
import lan.chaos.virtualthread.bench.BenchScenario;
import lan.chaos.virtualthread.common.constant.ExecutorMode;
import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.common.model.BenchCase;
import lan.chaos.virtualthread.common.model.BenchResult;
import lan.chaos.virtualthread.common.util.IoSimulator;

import java.util.List;

/**
 * 压测 B：线程池饱和 → 排队 → 拒绝。
 *
 * <p>WHY 这一组：真实服务配的是「有界队列 + 拒绝策略」，压满之后不是变慢，而是直接丢请求。
 * 这里让在途并发（2000）远超「线程数 100 + 队列 500」的承载上限，
 * 平台池会立刻触发 AbortPolicy 拒绝；虚拟线程没有队列上限，同样负载下零拒绝。
 * 关键不在吞吐数字，而在「拒绝数」这一列——它代表真实的用户请求损失。
 *
 * <p>注意：被拒绝的请求几乎瞬间返回，端到端延迟接近 0，
 * 所以平台池的 avg/p99 看起来并不差，这是「快速失败」的假象；
 * 读这一组必须把「成功数 / 拒绝数」和吞吐放在一起看。
 */
public class SaturationBench implements BenchScenario {

    private static final int TASK_COUNT = 10_000;
    private static final int CONCURRENCY = 2000;
    private static final int PLATFORM_THREADS = 100;
    private static final int QUEUE_CAPACITY = 500;
    private static final long IO_MILLIS = 50;

    @Override
    public Scenario id() {
        return Scenario.BENCH_SATURATION;
    }

    @Override
    public String conclusion() {
        return "承载上限（线程数 + 队列）被压穿后，平台池是「拒绝」，虚拟线程是「扛住」；"
                + "更隐蔽的是排队——无界队列下不拒绝但 p99 飙升，本质是把压力转成延迟。";
    }

    @Override
    public List<BenchCase> run() {
        BenchOptions options = BenchOptions.builder()
                .taskCount(TASK_COUNT)
                .concurrency(CONCURRENCY)
                .platformThreads(PLATFORM_THREADS)
                .queueCapacity(QUEUE_CAPACITY)
                .ioMillis(IO_MILLIS)
                .warmupRounds(1)
                .build();
        BenchEngine.BenchTask task = index -> IoSimulator.ioBlock(IO_MILLIS);

        BenchResult platform = BenchEngine.run("平台线程池(" + PLATFORM_THREADS + ",队列" + QUEUE_CAPACITY + ")",
                ExecutorMode.PLATFORM, options, task);
        BenchResult virtual = BenchEngine.run("虚拟线程(无队列上限)", ExecutorMode.VIRTUAL, options, task);

        return List.of(new BenchCase("在途并发 2000 远超承载上限 600", options.describe(),
                "平台线程池", "虚拟线程", platform, virtual));
    }
}
