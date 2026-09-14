package lan.chaos.pdf.read;

import lan.chaos.pdf.common.constant.PdfConstants;
import lan.chaos.pdf.common.util.OutFiles;
import lan.chaos.pdf.common.util.PdfKit;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 能力四：PDF 内容提取——把文字从 PDF 里「读回来」。
 *
 * <p><b>WHY（痛点）</b>：PDF 是「版面格式」而非「结构化文档」：文件里只有
 * 「在 (x,y) 处画字符串 S」这样的绘图指令，<b>没有段落、没有表格、没有阅读顺序</b>。
 * 所以提取本质上是在做「版面还原」，天生就是启发式的、脆弱的。
 *
 * <p><b>关键 API</b>：{@code PDFTextStripper#getText(PDDocument)}
 * + {@code setSortByPosition(boolean)}。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>不排序会乱序</b>：{@code setSortByPosition(false)}（默认）按
 *       <b>内容流的书写顺序</b>输出，而书写顺序取决于生成工具——
 *       双栏、表格、图注混排时会得到「右列在前、左列在后」这种颠倒结果。</li>
 *   <li><b>排序也不是万能的</b>：{@code true} 只是按 (y, x) 排序，遇到旋转页面、
 *       多栏跨页、上下标仍会串；复杂版面需要自己按坐标聚类还原。</li>
 *   <li><b>表格提取基本不可靠</b>：没有单元格概念，只能靠「空格数/坐标」猜，
 *       一旦列宽变化或内容换行就错位。<b>要做数据交换，别选 PDF</b>，用 Excel/CSV。</li>
 *   <li><b>扫描件提取不到任何文字</b>：图片型 PDF 必须先做 OCR。</li>
 * </ol>
 */
@Service
public class ReadService {

    private final OutFiles outFiles;

    public ReadService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File fontFile = PdfKit.findCjkFontFile();
        if (fontFile == null) {
            return "  跳过：未找到中文字体，无法演示文本提取。";
        }

        // 造一个「双栏、且故意先写右栏」的 PDF，用来暴露乱序坑
        File twoCol = buildTwoColumnPdf(fontFile);

        String unsorted;
        String sorted;
        try (PDDocument doc = Loader.loadPDF(twoCol)) {
            unsorted = extract(doc, false);
            sorted = extract(doc, true);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("  构造：双栏 PDF，且<b>故意</b>先在内容流里写右栏、再写左栏\n");
        sb.append("  setSortByPosition=false（默认）→ ").append(oneLine(unsorted)).append('\n');
        sb.append("  setSortByPosition=true        → ").append(oneLine(sorted)).append('\n');
        sb.append("  结论：默认按「内容流书写顺序」输出，双栏/表格必然乱序；开启排序才按阅读顺序还原\n");
        return sb.toString();
    }

    /** 读回：按是否按位置排序提取全文。 */
    public String extract(PDDocument doc, boolean sortByPosition) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(sortByPosition);
        return stripper.getText(doc);
    }

    /**
     * 造一个双栏 PDF：内容流里先写右栏（右栏靠右），再写左栏。
     *
     * <p>真实世界的排版工具（Word/WPS/LaTeX）输出顺序取决于自身实现，
     * 这里用「故意写反」来稳定复现乱序现象。
     */
    public File buildTwoColumnPdf(File fontFile) throws Exception {
        File out = outFiles.of("pdf-read-twocol.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDType0Font font = PdfKit.loadCjkFont(doc, fontFile);

            float pageW = page.getMediaBox().getWidth();
            float leftX = PdfConstants.MARGIN_X;
            float rightX = pageW / 2 + 10f;
            float y = PdfKit.startY(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // 先写右栏（阅读顺序上它应该在后面）
                PdfKit.drawText(cs, font, PdfConstants.BODY_SIZE, rightX, y, "右栏第一行");
                PdfKit.drawText(cs, font, PdfConstants.BODY_SIZE, rightX, y - 20, "右栏第二行");
                // 再写左栏
                PdfKit.drawText(cs, font, PdfConstants.BODY_SIZE, leftX, y, "左栏第一行");
                PdfKit.drawText(cs, font, PdfConstants.BODY_SIZE, leftX, y - 20, "左栏第二行");
            }
            doc.save(out);
        }
        return out;
    }

    /** 把提取结果压成一行便于对比（去掉换行与多余空白）。 */
    private static String oneLine(String text) {
        String t = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? "(空)" : t;
    }
}
