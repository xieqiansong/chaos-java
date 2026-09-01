package lan.chaos.virtualthread.runner;

import lan.chaos.virtualthread.bench.BenchRunner;

import java.nio.file.Path;

/**
 * 命令行压测入口：不依赖测试框架，直接跑完五个场景并生成报告。
 * 用法（模块目录下）：
 * <pre>
 *   mvn -q compile exec:java -Dexec.mainClass=lan.chaos.virtualthread.runner.BenchRunnerMain
 * </pre>
 * 若不想引 exec 插件，走 {@code mvn test -Dbench=true} 或 {@code mvn spring-boot:run -Dspring-boot.run.arguments=--bench} 亦可。
 */
public final class BenchRunnerMain {

    private BenchRunnerMain() {
    }

    public static void main(String[] args) {
        Path report = args.length > 0 ? Path.of(args[0]) : Path.of("target", "bench-results.md");
        BenchRunner.runAndWrite(report);
    }
}
