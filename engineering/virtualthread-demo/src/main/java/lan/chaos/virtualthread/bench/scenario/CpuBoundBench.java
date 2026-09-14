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
 * 压测 D：CPU 密集适用边界（虚拟线程不是万能的）。
 *
 * <p>WHY 这一组：虚拟线程只解决「等待」不解决「算力」。CPU 密集任务的瓶颈是核数，
 * 平台池按核数配线程已是较优解，虚拟线程再多也只是排队等着上核，
 * 还要额外付一层调度与栈内存开销。
 *
 * <p>参数刻意对齐最优：平台池线程数 = CPU 核数。若这样配置下两者吞吐仍基本持平，
 * 说明「CPU 密集场景换虚拟线程没有收益」——结论是**不要换**，而不是换得更差。
 * 实践中看到的几个百分点差异属于调度开销与测量噪声，不应解读为性能提升/劣化。
 */
public class CpuBoundBench implements BenchScenario {

    private static final long CPU_MILLIS = 2;

    @Override
    public Scenario id() {
        return Scenario.BENCH_CPU;
    }

    @Override
    public String conclusion() {
        return "CPU 密集下虚拟线程与核数对齐的平台池吞吐基本持平（几个百分点差异属噪声与调度开销），"
                + "瓶颈在核数不在线程模型——这类任务不该换虚拟线程。";
    }

    @Override
    public List<BenchCase> run() {
        int cores = Runtime.getRuntime().availableProcessors();
        int taskCount = cores * 250;
        BenchOptions options = BenchOptions.builder()
                .taskCount(taskCount)
                .concurrency(taskCount)
                .platformThreads(cores)
                .warmupRounds(1)
                .build();
        BenchEngine.BenchTask task = index -> IoSimulator.cpuBurn(CPU_MILLIS);

        BenchResult platform = BenchEngine.run("平台线程池(" + cores + "=核数)",
                ExecutorMode.PLATFORM, options, task);
        BenchResult virtual = BenchEngine.run("虚拟线程", ExecutorMode.VIRTUAL, options, task);

        return List.of(new BenchCase("纯计算任务 " + CPU_MILLIS + "ms × " + taskCount,
                options.describe(), "平台线程池(核数)", "虚拟线程", platform, virtual));
    }
}
