package lan.chaos.virtualthread.bench.scenario;

import lan.chaos.virtualthread.bench.BenchEngine;
import lan.chaos.virtualthread.bench.BenchOptions;
import lan.chaos.virtualthread.bench.BenchScenario;
import lan.chaos.virtualthread.common.constant.ExecutorMode;
import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.common.model.BenchCase;
import lan.chaos.virtualthread.common.model.BenchResult;
import lan.chaos.virtualthread.common.util.IoSimulator;

import java.util.ArrayList;
import java.util.List;

/**
 * 压测 A：IO 密集吞吐（最核心的一组）。
 *
 * <p>WHY 三档梯度：并发从 1000 → 2000（压力翻番）、单任务耗时 20ms → 100ms（下游变慢），
 * 观察差距如何被放大。理论值：平台池吞吐 ≈ 线程数/单任务耗时，与并发无关（多出来的全排队）；
 * 虚拟线程吞吐 ≈ 并发数/单任务耗时，随并发线性上涨。
 * 因此「并发越高、下游越慢，虚拟线程优势越大」——这正是线上高价值场景的形态。
 *
 * <p>平台池线程数固定 200：对齐 Tomcat 默认 maxThreads，是有现实意义的基线，
 * 而非刻意挑一个很小的线程池来夸大差距。
 */
public class IoIntensiveBench implements BenchScenario {

    private static final int PLATFORM_THREADS = 200;

    public IoIntensiveBench() {
    }

    @Override
    public Scenario id() {
        return Scenario.BENCH_IO;
    }

    @Override
    public String conclusion() {
        return "并发越高、下游越慢，虚拟线程优势越大：平台池吞吐被「线程数/单任务耗时」锁死，"
                + "超出的请求只能排队，先崩的是 p99；虚拟线程吞吐随并发线性上涨。";
    }

    @Override
    public List<BenchCase> run() {
        List<BenchCase> cases = new ArrayList<>();
        cases.add(runCase("并发 1000 / 单任务 IO 20ms", 4000, 1000, 20));
        cases.add(runCase("并发 2000 / 单任务 IO 20ms", 4000, 2000, 20));
        cases.add(runCase("并发 2000 / 单任务 IO 100ms", 4000, 2000, 100));
        return cases;
    }

    private BenchCase runCase(String name, int taskCount, int concurrency, long ioMillis) {
        BenchOptions options = BenchOptions.builder()
                .taskCount(taskCount)
                .concurrency(concurrency)
                .platformThreads(PLATFORM_THREADS)
                .ioMillis(ioMillis)
                .warmupRounds(1)
                .build();
        BenchEngine.BenchTask task = index -> IoSimulator.ioBlock(ioMillis);

        BenchResult platform = BenchEngine.run("平台线程池(" + PLATFORM_THREADS + ")",
                ExecutorMode.PLATFORM, options, task);
        BenchResult virtual = BenchEngine.run("虚拟线程", ExecutorMode.VIRTUAL, options, task);

        return new BenchCase(name, options.describe(),
                "平台线程池(" + PLATFORM_THREADS + ")", "虚拟线程", platform, virtual);
    }
}
