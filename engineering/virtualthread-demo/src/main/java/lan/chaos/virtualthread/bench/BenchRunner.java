package lan.chaos.virtualthread.bench;

import lan.chaos.virtualthread.bench.scenario.CpuBoundBench;
import lan.chaos.virtualthread.bench.scenario.HttpServerBench;
import lan.chaos.virtualthread.bench.scenario.IoIntensiveBench;
import lan.chaos.virtualthread.bench.scenario.PinningBench;
import lan.chaos.virtualthread.bench.scenario.SaturationBench;
import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.common.model.BenchCase;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 压测总入口：依次跑完五个场景，边跑边打印，最后汇总成 markdown 报告。
 * 测试（{@code BenchMarkTest}）与命令行（{@code BenchRunnerMain}）共用这一条路径，
 * 保证「跑测试得到的数字」和「跑命令得到的数字」是同一口径。
 */
public final class BenchRunner {

    private BenchRunner() {
    }

    public static List<BenchScenario> scenarios() {
        return List.of(new IoIntensiveBench(), new SaturationBench(), new PinningBench(),
                new CpuBoundBench(), new HttpServerBench());
    }

    public static Map<Scenario, List<BenchCase>> runAll() {
        Map<Scenario, List<BenchCase>> results = new LinkedHashMap<>();
        for (BenchScenario scenario : scenarios()) {
            Scenario id = scenario.id();
            System.out.println();
            System.out.println("========================================");
            System.out.println("压测场景: " + id.name() + " —— " + id.desc());
            System.out.println("----------------------------------------");
            long start = System.currentTimeMillis();
            List<BenchCase> cases = scenario.run();
            System.out.println();
            MarkdownReporter.print(cases);
            System.out.println("  耗时 " + (System.currentTimeMillis() - start) / 1000.0 + "s");
            System.out.println("  结论：" + scenario.conclusion());
            results.put(id, cases);
        }
        return results;
    }

    /** 跑完并把报告写到指定文件，返回全部结果（便于调用方直接做断言，避免重跑一遍矩阵）。 */
    public static Map<Scenario, List<BenchCase>> runAndWrite(Path reportFile) {
        Map<Scenario, List<BenchCase>> results = runAll();
        Path written = MarkdownReporter.write(reportFile, MarkdownReporter.renderAll(results));
        System.out.println();
        System.out.println("压测报告已生成: " + written.toAbsolutePath());
        return results;
    }
}
