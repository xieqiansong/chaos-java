package lan.chaos.word;

import lan.chaos.word.common.util.MavenVersion;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 版本矩阵测试：确认 POI 双构件（poi-ooxml 与 poi-scratchpad）运行期实际加载的版本一致，
 * 且都锁在平台 BOM 锁定的 5.5.1——Word 专题同时吃这两套 API，版本错位会直接 NoSuchMethodError。
 */
class VersionMatrixTest {

    private static final String EXPECTED = "5.5.1";

    @Test
    void poiOoxmlAndScratchpadBothResolveTo551() {
        assertEquals(EXPECTED, MavenVersion.of("org.apache.poi", "poi-ooxml", XWPFDocument.class),
                "XWPF 依赖的 poi-ooxml 必须锁在 5.5.1");
        assertEquals(EXPECTED, MavenVersion.of("org.apache.poi", "poi-scratchpad", HWPFDocument.class),
                "HWPF 依赖的 poi-scratchpad 必须锁在 5.5.1（与 poi-ooxml 同源，避免分裂）");
    }

    @Test
    void xmlbeansAndCompressPresent() {
        assertNotNull(MavenVersion.of("org.apache.xmlbeans", "xmlbeans",
                org.apache.xmlbeans.XmlObject.class), "xmlbeans 必须可用（OOXML XML Beans 基石）");
        assertNotNull(MavenVersion.of("org.apache.commons", "commons-compress",
                org.apache.commons.compress.archivers.zip.ZipFile.class), "commons-compress 必须可用");
    }
}
