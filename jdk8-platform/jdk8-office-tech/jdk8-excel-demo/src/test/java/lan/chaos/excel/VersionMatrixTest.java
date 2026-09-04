package lan.chaos.excel;

import com.alibaba.excel.EasyExcel;
import lan.chaos.excel.common.model.Order;
import lan.chaos.excel.common.util.MavenVersion;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【守门测试】依赖版本矩阵 + 三方库在锁定版本下的可用性。
 *
 * <p>WHY：Office 专题最容易翻车的地方不是 API 用法，而是 <b>版本错配</b>：
 * EasyExcel 4.0.3 编译期依赖 POI 5.2.5，本平台 BOM 已把 POI 统一锁到 5.5.1，
 * 也就是说 <b>EasyExcel 跑在它没编译过的 POI 版本上</b>。
 * 这类问题的症状是运行期 NoSuchMethodError / ClassNotFoundException，
 * 且堆栈落在 POI 内部类里，排查成本极高。
 *
 * <p>本测试做三件事：① 打印真实生效版本（可观察）；
 * ② 断言 poi 与 poi-ooxml 版本一致（防错配）；
 * ③ 让 EasyExcel 真实跑一次读写往返（防"能编译不能跑"）。
 */
class VersionMatrixTest {

    private static final String OUT = "target/out";

    @Test
    void printVersionMatrix() {
        String[] matrix = {
                // POI 系列：jar 是 OSGi 打包，无 META-INF/maven，只能从 MANIFEST 读版本
                MavenVersion.line("org.apache.poi", "poi", Workbook.class),
                MavenVersion.line("org.apache.poi", "poi-ooxml", XSSFWorkbook.class),
                MavenVersion.line("org.apache.xmlbeans", "xmlbeans", org.apache.xmlbeans.XmlObject.class),
                MavenVersion.line("org.apache.commons", "commons-compress",
                        org.apache.commons.compress.archivers.zip.ZipFile.class),
                MavenVersion.line("commons-io", "commons-io", org.apache.commons.io.IOUtils.class),
                MavenVersion.line("org.apache.logging.log4j", "log4j-api", org.apache.logging.log4j.LogManager.class),
                MavenVersion.line("com.alibaba", "easyexcel", EasyExcel.class),
                MavenVersion.line("org.ehcache", "ehcache", org.ehcache.Cache.class),
                MavenVersion.line("cn.hutool", "hutool-all", cn.hutool.poi.excel.ExcelUtil.class),
                MavenVersion.line("org.slf4j", "slf4j-api", org.slf4j.Logger.class),
        };
        System.out.println("\n========== 依赖版本矩阵（运行期实际生效） ==========");
        for (String line : matrix) {
            System.out.println("  " + line);
        }

        // 核对「Maven 仲裁结果」与「运行期实际加载的 jar」是否一致——两者不一致是 Office 依赖排查的经典盲区
        System.out.println("  -- 运行期实际加载的 jar --");
        System.out.println("  poi            -> " + MavenVersion.jarOf(Workbook.class));
        System.out.println("  commons-compress -> " + MavenVersion.jarOf(
                org.apache.commons.compress.archivers.zip.ZipFile.class));
        System.out.println("  commons-io     -> " + MavenVersion.jarOf(org.apache.commons.io.IOUtils.class));

        // POI 5.4+ 把完整 OOXML schema 拆成了 poi-ooxml-lite（默认）与 poi-ooxml-full（可选）：
        // lite 里有 spreadsheetml（Excel 够用），但没有 wordprocessingml（Word）——这就是"缺类"的根源。
        System.out.println("  poi-ooxml-lite 生效(spreadsheetml) : " + resourceExists(SPREADSHEETML_PROBE));
        System.out.println("  poi-ooxml-full 生效(wordprocessingml): " + resourceExists(WORDPROCESSINGML_PROBE));

        assertTrue(resourceExists(SPREADSHEETML_PROBE), "poi-ooxml-lite 必须在 classpath 上，否则 XSSF 无法读写 xlsx");
        assertTrue(MavenVersion.of("com.alibaba", "easyexcel").startsWith("4.0.3"), "EasyExcel 应为 4.0.3");
    }

    @Test
    void poiArtifactsMustShareSameVersion() {
        String poi = MavenVersion.of("org.apache.poi", "poi", Workbook.class);
        String ooxml = MavenVersion.of("org.apache.poi", "poi-ooxml", XSSFWorkbook.class);
        assertEquals(poi, ooxml,
                "poi 与 poi-ooxml 版本必须一致（当前 " + poi + " vs " + ooxml + "），否则运行期必现 NoSuchMethodError");
        assertEquals("5.5.1", poi,
                "平台 BOM 已把 POI 锁定为 5.5.1，EasyExcel 传递进来的 5.2.5 必须被收敛掉");
    }

    /**
     * 安全相关依赖不能被 Spring Boot BOM 悄悄压回老版本。
     *
     * <p>WHY：POI 5.4+ 的 zip 重复条目校验（CVE-2025-31672）与 zip bomb 防护依赖 commons-compress 的新行为，
     * 而 Spring Boot 2.7.18 的 BOM 会把 commons-compress 压到 1.23.0、commons-io 压到 2.12.0。
     * 被压回老版本 = 安全修复被悄悄吃掉，且不会有任何编译或启动报错。
     * 故本专题在 jdk8-office-tech 的 dependencyManagement 里把它们抬回 POI 官方基线，并用此测试守住。
     */
    @Test
    void securityCriticalDepsMustNotBeDowngradedBySpringBootBom() {
        assertEquals("1.28.0", MavenVersion.of("org.apache.commons", "commons-compress",
                        org.apache.commons.compress.archivers.zip.ZipFile.class),
                "commons-compress 必须保持在 1.28.0（POI 官方基线），被 BOM 压回 1.23.0 会丢掉 zip 安全修复");
        assertEquals("2.21.0", MavenVersion.of("commons-io", "commons-io", org.apache.commons.io.IOUtils.class),
                "commons-io 必须保持在 2.21.0（POI 官方基线）");
    }

    @Test
    void easyExcelRoundTripOnLockedPoiVersion() throws Exception {
        Files.createDirectories(Paths.get(OUT));
        File file = new File(OUT, "version-matrix-easyexcel.xlsx");

        List<Order> data = Order.samples(100);
        EasyExcel.write(file, Order.class).sheet("订单").doWrite(data);

        List<Order> back = EasyExcel.read(file).head(Order.class).sheet(0).doReadSync();
        assertEquals(100, back.size(), "EasyExcel 写入 100 行后应原样读回 100 行");
        assertEquals("SO-00001", back.get(0).getOrderNo());
        assertEquals("客户-1", back.get(0).getCustomer());
        // BigDecimal 用 compareTo 比较（忽略 scale 差异，按数值比较）
        assertEquals(0, data.get(0).getAmount().compareTo(back.get(0).getAmount()),
                "BigDecimal 金额往返后应保持一致");
        assertTrue(file.length() > 0, "产物文件应非空：" + file.getAbsolutePath());
    }

    private static final String SPREADSHEETML_PROBE =
            "org/openxmlformats/schemas/spreadsheetml/x2006/main/CTWorkbook.class";
    private static final String WORDPROCESSINGML_PROBE =
            "org/openxmlformats/schemas/wordprocessingml/x2006/main/CTDocument1.class";

    private static boolean resourceExists(String name) {
        return MavenVersion.class.getClassLoader().getResource(name) != null;
    }
}
