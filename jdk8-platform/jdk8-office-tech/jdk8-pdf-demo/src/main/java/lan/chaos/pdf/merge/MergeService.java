package lan.chaos.pdf.merge;

import lan.chaos.pdf.common.constant.PdfConstants;
import lan.chaos.pdf.common.util.OutFiles;
import lan.chaos.pdf.common.util.PdfKit;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 能力五：PDF 合并与拆分——报表场景里最常见的「后处理」操作。
 *
 * <p><b>WHY（痛点）</b>：批量导出时通常是「先按业务单元各自生成一份，最后合并成一本」。
 * PDFBox 提供了现成的 {@code PDFMergerUtility} / {@code Splitter}，
 * 但它们对<b>资源与内存</b>都有自己的脾气，用错就是 OOM 或文件损坏。
 *
 * <p><b>关键 API</b>：{@code PDFMergerUtility#addSource/#mergeDocuments}、
 * {@code Splitter#split}。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>3.0 的 mergeDocuments 参数变了</b>：它收的是
 *       {@code RandomAccessStreamCache.StreamCacheCreateFunction}，而
 *       {@code MemoryUsageSetting} 只是个<b>持有者</b>，必须取 {@code .streamCache} 字段：
 *       {@code mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly().streamCache)}。
 *       照抄 2.0 的 {@code mergeDocuments(null)} 在 3.0 会编译不过/抛错。</li>
 *   <li><b>大文件别全放内存</b>：合并上百兆 PDF 时用
 *       {@code setupTempFileOnly()} 走磁盘缓冲，避免 OOM。</li>
 *   <li><b>拆分出来的子文档要逐个 close</b>：{@code Splitter#split} 返回的
 *       每个 {@code PDDocument} 都持有资源，不关会泄漏文件句柄。</li>
 *   <li><b>合并后表单域/书签可能冲突</b>：同名 AcroForm 字段会被合并，
 *       书签需要自己重建。</li>
 * </ol>
 */
@Service
public class MergeService {

    private final OutFiles outFiles;

    public MergeService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File fontFile = PdfKit.findCjkFontFile();
        if (fontFile == null) {
            return "  跳过：未找到中文字体，无法演示合并拆分。";
        }

        // ① 先造 3 份分册
        List<File> parts = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            parts.add(buildPart(fontFile, i));
        }

        // ② 合并
        File merged = merge(parts);

        int mergedPages;
        try (PDDocument doc = Loader.loadPDF(merged)) {
            mergedPages = doc.getNumberOfPages();
        }

        // ③ 再按页拆开
        List<File> split = split(merged);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  合并：%d 份分册 → %s（%s，%d 页）%n",
                parts.size(), merged.getName(), OutFiles.readableSize(merged), mergedPages));
        sb.append(String.format("  拆分：%d 页 → %d 个单页文件%n", mergedPages, split.size()));
        sb.append("  注意：mergeDocuments 需传 MemoryUsageSetting.setupMainMemoryOnly().streamCache（3.0 新签名）\n");
        return sb.toString();
    }

    /** 造一份单页分册。 */
    private File buildPart(File fontFile, int index) throws Exception {
        File out = outFiles.of("pdf-merge-part-" + index + ".pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType0Font font = PdfKit.loadCjkFont(doc, fontFile);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PdfKit.drawText(cs, font, PdfConstants.TITLE_SIZE,
                        PdfConstants.MARGIN_X, PdfKit.startY(page), "第 " + index + " 分册");
                PdfKit.drawText(cs, font, PdfConstants.BODY_SIZE,
                        PdfConstants.MARGIN_X, PdfKit.startY(page) - 30, "这是用于演示合并的示例内容。");
            }
            doc.save(out);
        }
        return out;
    }

    /**
     * 合并多个 PDF。
     *
     * <p>关键点：{@code mergeDocuments} 要的是 {@code streamCache} 函数，
     * 不是 {@code MemoryUsageSetting} 本身（3.0 的签名变化）。
     */
    private File merge(List<File> sources) throws Exception {
        File out = outFiles.of("pdf-merged.pdf");
        PDFMergerUtility merger = new PDFMergerUtility();
        for (File f : sources) {
            merger.addSource(f);
        }
        merger.setDestinationFileName(out.getAbsolutePath());
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly().streamCache);
        return out;
    }

    /** 按页拆分；每个子文档用完必须 close（否则句柄泄漏）。 */
    private List<File> split(File source) throws Exception {
        List<File> result = new ArrayList<>();
        List<PDDocument> documents;
        try (PDDocument doc = Loader.loadPDF(source)) {
            Splitter splitter = new Splitter();
            splitter.setSplitAtPage(1);
            documents = splitter.split(doc);
        }
        int i = 1;
        try {
            for (PDDocument part : documents) {
                File out = outFiles.of("pdf-split-" + (i++) + ".pdf");
                part.save(out);
                result.add(out);
            }
        } finally {
            for (PDDocument part : documents) {
                part.close();
            }
        }
        return result;
    }
}
