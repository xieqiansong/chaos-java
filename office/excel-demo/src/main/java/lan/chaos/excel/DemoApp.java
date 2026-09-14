package lan.chaos.excel;

import com.alibaba.excel.EasyExcel;
import lan.chaos.excel.basic.BasicExcelService;
import lan.chaos.excel.bench.ExportBench;
import lan.chaos.excel.bench.ImportBench;
import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.common.util.MavenVersion;
import lan.chaos.excel.easyexcel.EasyExcelService;
import lan.chaos.excel.hutool.HutoolExcelService;
import lan.chaos.excel.importer.ImportCheckService;
import lan.chaos.excel.read.BigFileReadService;
import lan.chaos.excel.write.StyleWriteService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 控制台 Runner：分节打印每个能力的「输入 → 输出」，不写测试也能直接看效果。
 *
 * <pre>
 * mvn -pl jdk8-office-tech/jdk8-excel-demo test
 * mvn -pl jdk8-office-tech/jdk8-excel-demo exec:java -Dexec.mainClass=lan.chaos.excel.DemoApp
 * </pre>
 */
public class DemoApp {

    public static void main(String[] args) throws Exception {
        int benchRows = Integer.getInteger("bench.rows", ExcelConstants.BENCH_ROWS);
        try (ConfigurableApplicationContext ctx = SpringApplication.run(ExcelApplication.class, args)) {
            section("0. 依赖版本（运行期实际生效）", versions());
            section("1. 三剑客：HSSF / XSSF / SXSSF", ctx.getBean(BasicExcelService.class).run());
            section("2. 样式 · 公式 · 下拉 · 批注", ctx.getBean(StyleWriteService.class).run());
            section("3. 大文件读取：用户模型 vs SAX", ctx.getBean(BigFileReadService.class).run());
            section("4. EasyExcel：注解写 / 分批读 / 模板填充", ctx.getBean(EasyExcelService.class).run());
            section("5. 导入校验：好行入库，坏行列清单", ctx.getBean(ImportCheckService.class).run());
            section("6. Hutool 轻量封装（对照）", ctx.getBean(HutoolExcelService.class).run());
            section("7. 导出横评", ctx.getBean(ExportBench.class).run(benchRows).toMarkdown());
            section("8. 导入横评", ctx.getBean(ImportBench.class).run(benchRows).toMarkdown());
            System.out.println("产物目录：target/out（可用 Excel 直接打开验证）");
        }
    }

    private static String versions() {
        StringBuilder sb = new StringBuilder();
        sb.append("  poi              : ").append(MavenVersion.of("org.apache.poi", "poi", Workbook.class)).append('\n');
        sb.append("  poi-ooxml        : ").append(MavenVersion.of("org.apache.poi", "poi-ooxml", XSSFWorkbook.class)).append('\n');
        sb.append("  xmlbeans         : ").append(MavenVersion.of("org.apache.xmlbeans", "xmlbeans",
                org.apache.xmlbeans.XmlObject.class)).append('\n');
        sb.append("  commons-compress : ").append(MavenVersion.of("org.apache.commons", "commons-compress",
                org.apache.commons.compress.archivers.zip.ZipFile.class)).append('\n');
        sb.append("  easyexcel        : ").append(MavenVersion.of("com.alibaba", "easyexcel", EasyExcel.class)).append('\n');
        sb.append("  hutool-all       : ").append(MavenVersion.of("cn.hutool", "hutool-all",
                cn.hutool.poi.excel.ExcelUtil.class)).append('\n');
        return sb.toString();
    }

    private static void section(String title, String body) {
        System.out.println("\n========== " + title + " ==========");
        System.out.print(body);
    }
}
