package lan.chaos.pdf.bigdoc;

import lan.chaos.pdf.common.constant.PdfConstants;
import lan.chaos.pdf.common.util.OutFiles;
import lan.chaos.pdf.common.util.PdfCanvas;
import lan.chaos.pdf.common.util.PdfKit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * 能力六：大文档内存模型横评——看看页数与内存的正相关。
 *
 * <p><b>WHY（痛点）</b>：和 Word 一样，<b>PDFBox 也没有「流式写」模型</b>：
 * {@code PDDocument} 会把所有页面对象（含内容流）攒在内存里，最后一次性 {@code save}。
 * 这意味着页数越多内存越高，几千页的量级很容易 OOM。
 *
 * <p><b>关键 API</b>：仍是 {@code PDDocument}，重点在「用数据说话」——生成 N 页，记录耗时与内存增量。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>没有 SXSSF 那样的滑动窗口</b>：Excel 有流式写，Word/PDF 都没有。
 *       超大文档只能拆成多个子文件分别生成，再 {@code PDFMergerUtility} 合并。</li>
 *   <li><b>内存与页数近似线性</b>：本横评会直观展示这一点。</li>
 *   <li><b>合并时改用磁盘缓冲</b>：{@code MemoryUsageSetting.setupTempFileOnly()}，
 *       避免把整本再塞回内存。</li>
 * </ol>
 */
@Service
public class BigDocService {

    private final OutFiles outFiles;

    public BigDocService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run(int pages) throws Exception {
        Result r = generate(pages);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("| 方案 | 请求页数 | 实际页数 | 耗时(ms) | 内存增量(MB) | 文件大小 |%n"));
        sb.append(String.format("| --- | --- | --- | --- | --- | --- |%n"));
        // 注意报「实际页数」而不是请求页数：PdfCanvas 会在写不下时自动分页，
        // 两者并不天然相等（每轮内容高度超过一页容量时，实际页数会大于请求页数）。
        sb.append(String.format("| PDFBox(全内存) | %d | %d | %d | %d | %s |%n",
                pages, r.pages, r.elapsed, r.memDeltaMb, OutFiles.readableSize(r.file)));
        sb.append(String.format("%n  ⚠ PDF 同样没有流式写模型：PDDocument 会攒住所有页面内容直到 save。%n"
                + "    超大文档请「分批生成 + 合并」，合并时走 setupTempFileOnly() 用磁盘缓冲。%n"));
        return sb.toString();
    }

    /** 生成指定页数的文档，记录耗时与堆内存增量。 */
    public Result generate(int pages) throws Exception {
        File file = outFiles.of("pdf-big.pdf");
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long memBefore = mem.getHeapMemoryUsage().getUsed();
        long t0 = System.currentTimeMillis();

        File fontFile = PdfKit.findCjkFontFile();
        int actualPages = 0;
        try (PDDocument doc = new PDDocument()) {
            if (fontFile != null) {
                PDType0Font font = PdfKit.loadCjkFont(doc, fontFile);
                try (PdfCanvas c = new PdfCanvas(doc)) {
                    for (int p = 1; p <= pages; p++) {
                        c.text(font, PdfConstants.HEADING_SIZE, "第 " + p + " 页");
                        for (int l = 0; l < PdfConstants.BENCH_LINES_PER_PAGE; l++) {
                            c.text(font, PdfConstants.BODY_SIZE, PdfConstants.BENCH_TEXT);
                        }
                    }
                    // 取「自动分页后的真实页数」——它可能大于请求页数
                    actualPages = c.pageCount();
                }
            }
            doc.save(file);
        }

        long elapsed = System.currentTimeMillis() - t0;
        long memAfter = mem.getHeapMemoryUsage().getUsed();
        return new Result(elapsed, (memAfter - memBefore) / (1024 * 1024), file, actualPages);
    }

    /** 一次生成的结果（耗时 / 内存增量 / 产物 / 实际页数）。 */
    public static class Result {
        public final long elapsed;
        public final long memDeltaMb;
        public final File file;
        public final int pages;

        Result(long elapsed, long memDeltaMb, File file, int pages) {
            this.elapsed = elapsed;
            this.memDeltaMb = memDeltaMb;
            this.file = file;
            this.pages = pages;
        }
    }
}
