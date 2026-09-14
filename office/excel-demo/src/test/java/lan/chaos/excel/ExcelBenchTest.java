package lan.chaos.excel;

import lan.chaos.excel.bench.BenchResult;
import lan.chaos.excel.bench.ExportBench;
import lan.chaos.excel.bench.ImportBench;
import lan.chaos.excel.common.constant.ExcelConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 压测入口：跑导出 / 导入两组横评，结果落到 <code>target/bench-results.md</code>。
 *
 * <p>为什么性能结果<b>不做硬断言</b>：耗时与内存随机器、JDK、磁盘波动，
 * 硬断言只会让 CI 随机变红。这里只断言"横评真的跑完了、报告真的生成了"，
 * 具体数字留给人看（与仓库内其他压测 Demo 的取舍一致）。
 *
 * <p>想加大规模：<code>mvn test -Dbench.rows=100000</code>。
 */
@SpringBootTest
class ExcelBenchTest {

    @Autowired
    private ExportBench exportBench;
    @Autowired
    private ImportBench importBench;

    @Test
    void runExportAndImportBenches() throws Exception {
        int rows = Integer.getInteger("bench.rows", ExcelConstants.BENCH_ROWS);

        File report = new File("target/bench-results.md");
        Files.createDirectories(report.getParentFile().toPath());
        Files.deleteIfExists(report.toPath());

        BenchResult export = exportBench.run(rows);
        BenchResult anImport = importBench.run(rows);

        export.writeTo(report);
        anImport.appendTo(report);

        System.out.println("\n========== 横评报告 " + report.getAbsolutePath() + " ==========");
        System.out.println(new String(Files.readAllBytes(report.toPath()), StandardCharsets.UTF_8));

        assertTrue(report.length() > 0, "横评报告必须生成");
        assertTrue(export.toMarkdown().contains("SXSSF"), "导出横评应覆盖 SXSSF 方案");
        assertTrue(export.toMarkdown().contains("Hutool"), "导出横评应覆盖 Hutool 方案");
        assertTrue(anImport.toMarkdown().contains("SAX"), "导入横评应覆盖 SAX 方案");
        assertTrue(anImport.toMarkdown().contains("Hutool"), "导入横评应覆盖 Hutool 方案");
    }
}
