package lan.chaos.pdf.table;

import lan.chaos.pdf.common.constant.PdfConstants;
import lan.chaos.pdf.common.model.Statement;
import lan.chaos.pdf.common.model.StatementItem;
import lan.chaos.pdf.common.util.OutFiles;
import lan.chaos.pdf.common.util.PdfCanvas;
import lan.chaos.pdf.common.util.PdfKit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;

/**
 * 能力三：表格绘制——PDF 里<b>没有表格这个概念</b>，表格是「画」出来的。
 *
 * <p><b>WHY（痛点）</b>：Word/HTML 里表格是一等公民（{@code <table>} / {@code XWPFTable}），
 * PDF 里只有「文本」和「线段」。所谓表格 = 一堆 {@code moveTo/lineTo/stroke} 画出的网格
 * + 一堆按坐标摆好的文字。这带来两个必须自己解决的问题：
 * <ul>
 *   <li><b>跨页</b>：表格不会被自动拆到下一页，必须自己判断「这一行放不下就换页并重画表头」。</li>
 *   <li><b>列宽</b>：没有 {@code auto-fit}，列宽要么写死，要么用
 *       {@code font.getStringWidth} 量出内容最大宽度再定。</li>
 * </ul>
 *
 * <p><b>关键 API</b>：{@code PDPageContentStream#addRect / moveTo / lineTo / stroke}
 * + {@link PdfKit#textWidth}。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>跨页不重画表头</b>：第二页起表格没有列名，读者完全看不懂。</li>
 *   <li><b>坐标算错导致「最后一行消失」</b>：Y 减过头，行画到了页边距外，PDF 不报错。</li>
 *   <li><b>单元格合并</b>：没有 {@code merge}，只能「不画那条内边框」来视觉模拟，
 *       导出的数据语义仍然是散的（这是 PDF 不适合做结构化导出的根本原因）。</li>
 * </ol>
 */
@Service
public class TablePdfService {

    private static final String[] HEADERS = {"项目", "负责人", "状态", "金额(元)"};
    private static final float[] COL_RATIO = {0.34f, 0.22f, 0.20f, 0.24f};

    private final OutFiles outFiles;

    public TablePdfService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File fontFile = PdfKit.findCjkFontFile();
        if (fontFile == null) {
            return "  跳过：未找到中文字体，无法演示表格绘制。";
        }
        File file = build(fontFile);
        int pages;
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(file)) {
            pages = doc.getNumberOfPages();
        }
        return String.format("  生成 %s（%s，%d 页）%n"
                        + "  演示：网格线表格 / 列宽按比例分配 / 跨页自动重画表头 / 表头底色 / 合计行%n",
                file.getName(), OutFiles.readableSize(file), pages);
    }

    public File build(File fontFile) throws Exception {
        File out = outFiles.of("pdf-table.pdf");
        Statement statement = Statement.sample();

        try (PDDocument doc = new PDDocument()) {
            PDType0Font cjk = PdfKit.loadCjkFont(doc, fontFile);
            // 造足够多的行，逼出「跨页」场景
            try (PdfCanvas c = new PdfCanvas(doc)) {
                float tableW = PdfKit.contentWidth(c.page());
                drawTable(c, cjk, statement, tableW);
            }
            doc.save(out);
        }
        return out;
    }

    /** 画完整表格：表头 + 明细行 + 合计行，跨页自动重画表头。 */
    private void drawTable(PdfCanvas c, PDType0Font font, Statement statement, float tableW) throws Exception {
        float rowH = PdfConstants.ROW_HEIGHT;
        float x0 = PdfConstants.MARGIN_X;

        // 第一行先放表头
        drawRow(c, font, x0, tableW, rowH, HEADERS, true);

        for (StatementItem item : statement.getItems()) {
            // 坑：放不下就换页，并且必须把表头再画一遍
            if (PdfKit.needNewPage(c.y() - rowH)) {
                c.newPage();
                drawRow(c, font, x0, tableW, rowH, HEADERS, true);
            }
            drawRow(c, font, x0, tableW, rowH,
                    new String[]{item.getProject(), item.getOwner(), item.getStatus(), item.amountYuan()},
                    false);
        }

        // 合计行
        if (PdfKit.needNewPage(c.y() - rowH)) {
            c.newPage();
            drawRow(c, font, x0, tableW, rowH, HEADERS, true);
        }
        drawRow(c, font, x0, tableW, rowH,
                new String[]{"合计", "", "", String.format("%.2f", statement.totalCents() / 100.0)},
                true);
    }

    /** 画一行：先写文字，再补网格线。 */
    private void drawRow(PdfCanvas c, PDType0Font font, float x0, float tableW,
                         float rowH, String[] cells, boolean isHeader) throws Exception {
        float top = c.y();
        float textSize = PdfConstants.TABLE_SIZE;

        if (isHeader) {
            PdfKit.fillRect(c.cs(), x0, top - rowH, tableW, rowH, new Color(222, 235, 247));
        }

        // 单元格文字：按列比例定位，金额列右对齐（用 getStringWidth 反算 x）
        float x = x0;
        for (int i = 0; i < cells.length; i++) {
            float colW = tableW * COL_RATIO[i];
            String text = cells[i] == null ? "" : cells[i];
            float tx = x + 4f;
            if (i == cells.length - 1) {
                // 末列右对齐
                tx = x + colW - 4f - PdfKit.textWidth(font, textSize, text);
            }
            // 基线：行垂直居中
            float baseline = top - rowH / 2 - textSize / 3;
            PdfKit.drawText(c.cs(), font, textSize, tx, baseline, text);
            x += colW;
        }

        // 网格线
        c.cs().setStrokingColor(Color.GRAY);
        c.cs().setLineWidth(0.6f);
        float cx = x0;
        for (float ratio : COL_RATIO) {
            PdfKit.drawLine(c.cs(), cx, top, cx, top - rowH);
            cx += tableW * ratio;
        }
        PdfKit.drawLine(c.cs(), cx, top, cx, top - rowH);          // 右边界
        PdfKit.drawLine(c.cs(), x0, top, x0 + tableW, top);        // 上边
        PdfKit.drawLine(c.cs(), x0, top - rowH, x0 + tableW, top - rowH); // 下边

        c.setY(top - rowH);
    }
}
