package lan.chaos.word;

import lan.chaos.word.basic.BasicWordService;
import lan.chaos.word.bigdoc.BigDocService;
import lan.chaos.word.common.constant.WordConstants;
import lan.chaos.word.common.util.MavenVersion;
import lan.chaos.word.read.ReadService;
import lan.chaos.word.style.StyleService;
import lan.chaos.word.table.TableService;
import lan.chaos.word.template.TemplateFillService;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 控制台 Runner：分节打印每个能力的「输入 → 输出」，不写测试也能直接看效果。
 *
 * <pre>
 * mvn -pl jdk8-office-tech/jdk8-word-demo test
 * mvn -pl jdk8-office-tech/jdk8-word-demo package -DskipTests &amp;&amp; java -jar target/jdk8-word-demo-1.0-SNAPSHOT.jar
 * </pre>
 */
public class DemoApp {

    public static void main(String[] args) throws Exception {
        int bench = Integer.getInteger("bench.rows", WordConstants.BENCH_PARAGRAPHS);
        try (ConfigurableApplicationContext ctx = SpringApplication.run(WordApplication.class, args)) {
            section("0. 依赖版本（运行期实际生效）", versions());
            section("1. XWPF 结构化文档", ctx.getBean(BasicWordService.class).run());
            section("2. 样式与中文字体（EastAsia 坑）", ctx.getBean(StyleService.class).run());
            section("3. 复杂表格（合并/列宽/背景）", ctx.getBean(TableService.class).run());
            section("4. 读取：XWPF(docx) vs HWPF(.doc)", ctx.getBean(ReadService.class).run());
            section("5. 模板填充（跨 run 坑 + 表格行复制）", ctx.getBean(TemplateFillService.class).run());
            section("6. 大文档内存模型横评", ctx.getBean(BigDocService.class).run(bench));
            System.out.println("产物目录：target/out（可用 Word / WPS 打开验证）");
        }
    }

    private static String versions() {
        StringBuilder sb = new StringBuilder();
        sb.append("  poi-ooxml      : ").append(MavenVersion.of("org.apache.poi", "poi-ooxml", XWPFDocument.class)).append('\n');
        sb.append("  poi-scratchpad : ").append(MavenVersion.of("org.apache.poi", "poi-scratchpad", HWPFDocument.class)).append('\n');
        sb.append("  xmlbeans       : ").append(MavenVersion.of("org.apache.xmlbeans", "xmlbeans",
                org.apache.xmlbeans.XmlObject.class)).append('\n');
        sb.append("  commons-compress: ").append(MavenVersion.of("org.apache.commons", "commons-compress",
                org.apache.commons.compress.archivers.zip.ZipFile.class)).append('\n');
        return sb.toString();
    }

    private static void section(String title, String body) {
        System.out.println("\n========== " + title + " ==========");
        System.out.print(body);
    }
}
