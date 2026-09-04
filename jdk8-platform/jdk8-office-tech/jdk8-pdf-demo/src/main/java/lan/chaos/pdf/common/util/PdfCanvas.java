package lan.chaos.pdf.common.util;

import lan.chaos.pdf.common.constant.PdfConstants;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * 「页面游标」：把 PDFBox 最啰嗦的三件事（坐标推进、自动分页、内容流生命周期）封装掉。
 *
 * <p>WHY 需要它：PDF 没有布局引擎，写多行内容时每个 service 都要重复同一套动作——
 * 算 Y、判断要不要换页、close 旧内容流、new 页面、new 内容流、重置 Y。
 * 这段逻辑一旦写错，最难排查的症状是「内容凭空消失」：
 * 因为 <b>内容流不 {@code close()} 就不会真正写入页面</b>，而 PDFBox 对此不报错。
 *
 * <p>用法：
 * <pre>{@code
 * try (PdfCanvas c = new PdfCanvas(doc)) {
 *     c.text(font, 12f, "标题");
 *     c.paragraph(font, 11f, "一段很长的正文……");  // 自动换行 + 自动分页
 * }
 * }</pre>
 */
public class PdfCanvas implements Closeable {

    private final PDDocument doc;
    private PDPage page;
    private PDPageContentStream cs;
    private float y;
    private int pageCount;

    /** 建一个 A4 画布，并落下第一页。 */
    public PdfCanvas(PDDocument doc) throws IOException {
        this.doc = doc;
        newPage();
    }

    /**
     * 换页。
     *
     * <p><b>坑</b>：必须先 close 当前内容流再建新页面——
     * 内容流与页面是绑定的，未 close 的流不会写入，内容直接丢失。
     */
    public void newPage() throws IOException {
        if (cs != null) {
            cs.close();
            cs = null;
        }
        page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = PdfKit.startY(page);
        pageCount++;
    }

    /** 若剩余空间不足 {@code neededHeight}，先换页。 */
    public void ensure(float neededHeight) throws IOException {
        if (PdfKit.needNewPage(y - neededHeight)) {
            newPage();
        }
    }

    /** 在当前 Y 处写一行（不换行），并把 Y 下移 {@code lineHeight}。 */
    public void text(PDFont font, float size, String line, float lineHeight) throws IOException {
        ensure(lineHeight);
        PdfKit.drawText(cs, font, size, PdfConstants.MARGIN_X, y, line);
        y -= lineHeight;
    }

    /** 在当前 Y 处写一行（不换行），行高按字号 1.5 倍。 */
    public void text(PDFont font, float size, String line) throws IOException {
        text(font, size, line, size * 1.5f);
    }

    /**
     * 写一个段落：按可用宽度自动换行，跨页时自动续页。
     *
     * <p><b>坑</b>：PDF 里没有「文本框自动换行」，必须自己按 {@link PdfKit#textWidth} 逐字量。
     */
    public void paragraph(PDFont font, float size, String text) throws IOException {
        paragraph(font, size, text, size * 1.5f);
    }

    /** 写一个段落（可指定行高）。 */
    public void paragraph(PDFont font, float size, String text, float lineHeight) throws IOException {
        List<String> lines = PdfKit.wrap(font, size, text, PdfKit.contentWidth(page));
        for (String line : lines) {
            text(font, size, line, lineHeight);
        }
    }

    /** 在距左边距 {@code dx} 处写一行（列表缩进用）。 */
    public void textAt(PDFont font, float size, String line, float dx, float lineHeight) throws IOException {
        ensure(lineHeight);
        PdfKit.drawText(cs, font, size, PdfConstants.MARGIN_X + dx, y, line);
        y -= lineHeight;
    }

    /** 手动下移 {@code height}（留白用）。 */
    public void gap(float height) throws IOException {
        ensure(height);
        y -= height;
    }

    /** 画一条水平分隔线。 */
    public void hr() throws IOException {
        ensure(12f);
        cs.setStrokingColor(java.awt.Color.LIGHT_GRAY);
        cs.setLineWidth(0.7f);
        PdfKit.drawLine(cs, PdfConstants.MARGIN_X, y,
                PdfConstants.MARGIN_X + PdfKit.contentWidth(page), y);
        y -= 12f;
    }

    public float y() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public PDPage page() {
        return page;
    }

    public PDPageContentStream cs() {
        return cs;
    }

    public int pageCount() {
        return pageCount;
    }

    /** 关闭当前内容流；不调用则最后一页内容全部丢失。 */
    @Override
    public void close() throws IOException {
        if (cs != null) {
            cs.close();
            cs = null;
        }
    }
}
