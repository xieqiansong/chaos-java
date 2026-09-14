package lan.chaos.pdf;

import lan.chaos.pdf.common.util.MavenVersion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 版本矩阵测试：确认 PDFBox 三件套（pdfbox / fontbox / pdfbox-io）运行期实际加载的版本一致，
 * 且都锁在平台 BOM 的 3.0.6。
 *
 * <p>WHY 这条测试很关键：PDFBox 3.0 相对 2.0 是<b>破坏性升级</b>
 * （{@code PDDocument.load} → {@code Loader.loadPDF}、{@code PDType1Font.HELVETICA} 常量被移除）。
 * 而 2.0.x 在 Maven 生态里仍被大量构件传递引用——一旦 classpath 上混入 2.0.x，
 * 症状是运行期 NoSuchMethodError，且堆栈落在 PDFBox 内部类里，极难定位。
 */
class VersionMatrixTest {

    private static final String EXPECTED = "3.0.6";

    @Test
    void pdfboxResolvesTo306() {
        assertEquals(EXPECTED, MavenVersion.of("org.apache.pdfbox", "pdfbox", PDDocument.class),
                "pdfbox 必须锁在 3.0.6");
    }

    @Test
    void fontboxAndPdfboxIoShareSameVersion() {
        assertEquals(EXPECTED, MavenVersion.of("org.apache.pdfbox", "fontbox",
                        org.apache.fontbox.ttf.TrueTypeFont.class),
                "fontbox 必须与 pdfbox 同版本（字体解析由它负责）");
        assertEquals(EXPECTED, MavenVersion.of("org.apache.pdfbox", "pdfbox-io",
                        org.apache.pdfbox.io.MemoryUsageSetting.class),
                "pdfbox-io 必须与 pdfbox 同版本（随机访问缓冲由它负责）");
    }

    @Test
    void noPdfbox2xOnClasspath() {
        String pdfbox = MavenVersion.of("org.apache.pdfbox", "pdfbox", PDDocument.class);
        assertTrue(pdfbox.startsWith("3."),
                "classpath 上混入 PDFBox 2.x 会导致 3.0 新 API 全部失效，实际=" + pdfbox);
        assertNotNull(MavenVersion.ofJar(PDDocument.class), "应能定位到 pdfbox jar");
    }
}
