package lan.chaos.pdf;

import lan.chaos.pdf.basic.BasicPdfService;
import lan.chaos.pdf.bigdoc.BigDocService;
import lan.chaos.pdf.common.config.PdfProperties;
import lan.chaos.pdf.common.constant.PdfConstants;
import lan.chaos.pdf.common.util.OutFiles;
import lan.chaos.pdf.common.util.PdfKit;
import lan.chaos.pdf.font.FontService;
import lan.chaos.pdf.merge.MergeService;
import lan.chaos.pdf.read.ReadService;
import lan.chaos.pdf.table.TablePdfService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PDF 专题场景测试：每个能力都「生成 → 用 PDFBox 重新读回 → 断言关键结果」，
 * 不依赖肉眼打开（但产物都真实落盘到 target/out，可用 PDF 阅读器人工验证）。
 *
 * <p><b>关于中文字体</b>：中文相关用例依赖系统里装有中文字体（Windows 一般有，
 * Linux 容器未必）。找不到字体的环境下这些用例会被 {@code assumeTrue} 跳过，
 * 而不是标红失败——「缺字体」是部署问题，不是代码缺陷。
 */
class PdfScenarioTest {

    private final OutFiles outFiles = new OutFiles(new PdfProperties());
    private final FontService font = new FontService(outFiles);
    private final BasicPdfService basic = new BasicPdfService(outFiles);
    private final TablePdfService table = new TablePdfService(outFiles);
    private final ReadService read = new ReadService(outFiles);
    private final MergeService merge = new MergeService(outFiles);
    private final BigDocService bigdoc = new BigDocService(outFiles);

    @Test
    void font_standard14CannotWriteChinese() throws Exception {
        // 这一条不依赖任何系统字体：标准 14 字体里根本没有中文字形，必然失败
        String out = font.run();
        assertTrue(out.contains("失败（预期内）"),
                "标准14字体(Helvetica)写中文应当失败，实际输出：" + out);
        // 断言码点而不是整句文案：PDFBox 2.x 是 "No glyph for U+4E2D"，
        // 3.0.x 改成了 "U+4E2D ('.notdef') is not available in the font"，只有码点是稳定的。
        assertTrue(out.contains("U+4E2D"),
                "异常里应指出缺失的码点 U+4E2D（中），实际输出：" + out);
    }

    @Test
    void font_cjkEmbeddingWorks() throws Exception {
        requireCjkFont();
        font.run();
        File file = outFiles.of("pdf-font-cjk.pdf");
        assertTrue(file.exists(), "中文字体 PDF 应已落盘");
        assertTrue(file.length() > 0, "文件不应为空");
    }

    @Test
    void basic_generatesMultiPagePdf() throws Exception {
        requireCjkFont();
        basic.run();
        File file = outFiles.of("pdf-basic.pdf");
        try (PDDocument doc = Loader.loadPDF(file)) {
            // 内容里特意写了 40 行，足以触发自动分页
            assertTrue(doc.getNumberOfPages() >= 2, "应触发自动分页，实际页数=" + doc.getNumberOfPages());
        }
    }

    @Test
    void table_generatesPdf() throws Exception {
        requireCjkFont();
        table.run();
        File file = outFiles.of("pdf-table.pdf");
        assertTrue(file.exists(), "表格 PDF 应已落盘");
        try (PDDocument doc = Loader.loadPDF(file)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    @Test
    void read_sortByPositionChangesOrder() throws Exception {
        requireCjkFont();
        File twoCol = read.buildTwoColumnPdf(PdfKit.findCjkFontFile());
        try (PDDocument doc = Loader.loadPDF(twoCol)) {
            String unsorted = read.extract(doc, false);
            String sorted = read.extract(doc, true);

            // 内容流里是「先写右栏」，所以不排序时右栏在前（乱序）
            assertTrue(unsorted.indexOf("右栏") < unsorted.indexOf("左栏"),
                    "不排序时应按内容流书写顺序输出（右栏在前），实际=" + unsorted.replaceAll("\\s+", " "));
            // 开启排序后按坐标还原阅读顺序（左栏在前）
            assertTrue(sorted.indexOf("左栏") < sorted.indexOf("右栏"),
                    "开启 setSortByPosition 后应按阅读顺序输出（左栏在前），实际=" + sorted.replaceAll("\\s+", " "));
        }
    }

    @Test
    void merge_mergeThenSplit() throws Exception {
        requireCjkFont();
        merge.run();
        File merged = outFiles.of("pdf-merged.pdf");
        int pages;
        try (PDDocument doc = Loader.loadPDF(merged)) {
            pages = doc.getNumberOfPages();
        }
        assertEquals(3, pages, "3 份单页分册合并后应为 3 页");
        // 拆分应逐页产出
        assertTrue(outFiles.of("pdf-split-1.pdf").exists(), "拆分产物 1 应存在");
        assertTrue(outFiles.of("pdf-split-3.pdf").exists(), "拆分产物 3 应存在");
    }

    @Test
    void bigdoc_generatesWithoutError() throws Exception {
        requireCjkFont();
        BigDocService.Result r = bigdoc.generate(PdfConstants.BENCH_PAGES);
        assertTrue(r.file.exists(), "压测产物应已落盘");
        assertTrue(r.elapsed >= 0, "耗时应非负");
        // BENCH_LINES_PER_PAGE 已按 A4 可用高度调好，请求 N 页应恰好排出 N 页；
        // 用 >= 而非 ==，避免以后微调字号/边距就把用例搞红。
        assertTrue(r.pages >= PdfConstants.BENCH_PAGES,
                "实际页数应不少于请求页数，请求=" + PdfConstants.BENCH_PAGES + " 实际=" + r.pages);
    }

    /** 需要中文字体；没有则跳过（部署环境问题，不算代码缺陷）。 */
    private static void requireCjkFont() {
        assumeTrue(PdfKit.findCjkFontFile() != null,
                "未找到系统中文字体，跳过该用例（Windows 一般有，Linux 容器需自行安装 fonts-noto-cjk）");
    }
}
