package lan.chaos.virtualthread.bench;

import lan.chaos.virtualthread.common.constant.Scenario;
import lan.chaos.virtualthread.common.model.BenchCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 把压测结果渲染成 markdown 报告。
 * WHY 自动生成：手写数据表格既易抄错、又无法随代码复跑更新；
 * 让压测自己产出报告，笔记里引用的数据才对得上代码。
 */
public final class MarkdownReporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private MarkdownReporter() {
    }

    public static String renderAll(Map<Scenario, List<BenchCase>> grouped) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 压测明细\n");
        sb.append("\n> 本报告由 `BenchMarkTest` 自动生成：").append(LocalDateTime.now().format(FMT)).append("\n");
        sb.append("> 环境：JDK ").append(System.getProperty("java.version"))
                .append(" / ").append(System.getProperty("os.name"))
                .append(" / CPU 核数 ").append(Runtime.getRuntime().availableProcessors()).append("\n");

        sb.append("\n### 结论速览\n\n");
        sb.append("| 场景 | 用例 | 基线吞吐(任务/s) | 对照吞吐(任务/s) | 吞吐提升 | p99 改善 |\n");
        sb.append("|------|------|------------------|------------------|----------|----------|\n");
        for (Map.Entry<Scenario, List<BenchCase>> e : grouped.entrySet()) {
            for (BenchCase c : e.getValue()) {
                sb.append(String.format("| %s | %s | %,.0f | %,.0f | %.2f 倍 | %.2f 倍 |%n",
                        e.getKey().name(), c.name(),
                        c.baseline().throughputPerSec(), c.comparison().throughputPerSec(),
                        c.throughputGain(), c.p99Gain()));
            }
        }

        for (Map.Entry<Scenario, List<BenchCase>> e : grouped.entrySet()) {
            Scenario scenario = e.getKey();
            sb.append("\n### ").append(scenario.name()).append("：").append(scenario.desc()).append("\n");
            for (BenchCase c : e.getValue()) {
                sb.append("\n**").append(c.name()).append("** —— 参数：").append(c.params()).append("\n\n");
                sb.append("| 模式 | 吞吐(任务/s) | 总耗时(ms) | 平均延迟(ms) | p50(ms) | p99(ms) | 最大(ms) | 成功 | 失败 | 拒绝 |\n");
                sb.append("|------|--------------|------------|--------------|---------|---------|----------|------|------|------|\n");
                sb.append(c.baseline().tableRow()).append(" *(基线)*\n");
                sb.append(c.comparison().tableRow()).append(" *(对照)*\n");
            }
        }
        return sb.toString();
    }

    /** 控制台打印：一行一个用例，便于边跑边看。 */
    public static void print(List<BenchCase> cases) {
        for (BenchCase c : cases) {
            System.out.println(c);
        }
    }

    public static Path write(Path file, String markdown) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, markdown, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("写压测报告失败: " + file, e);
        }
    }
}
