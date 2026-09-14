package lan.chaos.virtualthread.bench;

import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.common.model.BenchCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 一键压测：跑满五个场景并把结果写成 markdown 报告。
 *
 * <p>WHY 默认不跑：完整矩阵是分钟级，混进日常 {@code mvn test} 会拖慢反馈；
 * 且压测数据受机器负载影响，不适合做 CI 硬门禁。需要时显式触发：
 * <pre>
 *   mvn test -Dbench=true
 * </pre>
 * 断言只卡「方向性结论」（虚拟线程明显更快、pinning 明显更慢），不卡绝对数值，避免机器差异导致误报。
 */
@Tag("bench")
class BenchMarkTest {

    @Test
    void runFullBenchmark_andWriteReport() {
        Path report = Path.of("target", "bench-results.md");
        Map<Scenario, List<BenchCase>> results = BenchRunner.runAndWrite(report);

        assertTrue(Files.exists(report), "压测报告应生成");

        // A：IO 密集——虚拟线程吞吐应明显高于平台线程池
        List<BenchCase> ioCases = results.get(Scenario.BENCH_IO);
        assertTrue(ioCases.stream().allMatch(c -> c.throughputGain() > 1.5),
                "IO 密集场景虚拟线程吞吐应至少高出 50%");

        // C：pinning——synchronized 版本应明显慢于 ReentrantLock 版本
        List<BenchCase> pinningCases = results.get(Scenario.BENCH_PINNING);
        assertTrue(pinningCases.stream().allMatch(c -> c.throughputGain() > 1.5),
                "pinning 场景 ReentrantLock 吞吐应至少高出 50%");

        // B：饱和——虚拟线程零拒绝，平台线程池出现拒绝
        List<BenchCase> saturationCases = results.get(Scenario.BENCH_SATURATION);
        assertTrue(saturationCases.stream().allMatch(c -> c.baseline().rejected() > 0),
                "平台线程池在超出承载上限时应出现拒绝");
        assertTrue(saturationCases.stream().allMatch(c -> c.comparison().rejected() == 0),
                "虚拟线程在同等负载下应零拒绝");
    }
}
