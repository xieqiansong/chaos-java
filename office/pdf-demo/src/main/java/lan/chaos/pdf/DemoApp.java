package lan.chaos.pdf;

import lan.chaos.pdf.basic.BasicPdfService;
import lan.chaos.pdf.bigdoc.BigDocService;
import lan.chaos.pdf.common.constant.PdfConstants;
import lan.chaos.pdf.common.util.MavenVersion;
import lan.chaos.pdf.font.FontService;
import lan.chaos.pdf.merge.MergeService;
import lan.chaos.pdf.read.ReadService;
import lan.chaos.pdf.table.TablePdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 控制台 Runner：分节打印每个能力的「输入 → 输出」，不写测试也能直接看效果。
 *
 * <pre>
 * mvn -pl jdk8-office-tech/jdk8-pdf-demo test
 * mvn -pl jdk8-office-tech/jdk8-pdf-demo package -DskipTests &amp;&amp; java -jar target/jdk8-pdf-demo-1.0-SNAPSHOT.jar
 * </pre>
 */
public class DemoApp {

    public static void main(String[] args) throws Exception {
        int bench = Integer.getInteger("bench.pages", PdfConstants.BENCH_PAGES);
        try (ConfigurableApplicationContext ctx = SpringApplication.run(PdfApplication.class, args)) {
            section("0. 依赖版本（运行期实际生效）", versions());
            section("1. 中文字体嵌入（PDF 第一大坑）", ctx.getBean(FontService.class).run());
            section("2. 结构化文档绘制（坐标系 / 自动分页）", ctx.getBean(BasicPdfService.class).run());
            section("3. 表格绘制（PDF 里没有表格，全是画出来的）", ctx.getBean(TablePdfService.class).run());
            section("4. 内容提取（PDFTextStripper 乱序坑）", ctx.getBean(ReadService.class).run());
            section("5. 合并与拆分", ctx.getBean(MergeService.class).run());
            section("6. 大文档内存模型横评", ctx.getBean(BigDocService.class).run(bench));
            System.out.println("产物目录：target/out（可用任意 PDF 阅读器打开验证）");
        }
    }

    private static String versions() {
        StringBuilder sb = new StringBuilder();
        sb.append("  pdfbox    : ").append(MavenVersion.of("org.apache.pdfbox", "pdfbox", PDDocument.class)).append('\n');
        sb.append("  fontbox   : ").append(MavenVersion.of("org.apache.pdfbox", "fontbox",
                org.apache.fontbox.ttf.TrueTypeFont.class)).append('\n');
        sb.append("  pdfbox-io : ").append(MavenVersion.of("org.apache.pdfbox", "pdfbox-io",
                org.apache.pdfbox.io.MemoryUsageSetting.class)).append('\n');
        return sb.toString();
    }

    private static void section(String title, String body) {
        System.out.println("\n========== " + title + " ==========");
        System.out.print(body);
    }
}
