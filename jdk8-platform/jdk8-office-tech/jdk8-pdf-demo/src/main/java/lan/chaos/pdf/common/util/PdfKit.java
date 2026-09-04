package lan.chaos.pdf.common.util;

import lan.chaos.pdf.common.constant.PdfConstants;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDFBox 的「踩坑才学到」底层 API 集中放这里：全模块共用，复制即用。
 *
 * <p>WHY 单列一个工具类：PDF 与 Word/Excel 最大的世界观差异是——
 * <b>PDF 没有布局引擎</b>。没有「段落」、没有「自动换行」、没有「分页」：
 * 每个字的坐标、每一行的宽度、每一页的切换，全都要程序自己算。
 * 这些计算散落在各 service 里既重复又易错，收口到一处能让读者一眼看清
 * 「哪些是 PDF 的原生坑」。
 */
public final class PdfKit {

    private PdfKit() {
    }

    // ---------------------------------------------------------------- 字体

    /**
     * 在当前系统里探测一个可用的中文字体文件；找不到返回 {@code null}。
     *
     * <p>WHY 不能随包带字体：中文字体 10~20 MB 且授权各异，塞进仓库不合理。
     * 生产做法是「字体作为部署物」（镜像内安装 / 挂载固定路径），代码只负责探测。
     */
    public static File findCjkFontFile() {
        for (String path : PdfConstants.CJK_FONT_CANDIDATES) {
            File f = new File(path);
            if (f.isFile() && f.length() > 0) {
                return f;
            }
        }
        return null;
    }

    /**
     * 加载中文字体，返回可写中文的 {@link PDType0Font}。
     *
     * <p><b>坑1（最大的坑）</b>：PDF 的「标准 14 字体」（Helvetica / Times 等）里
     * <b>根本没有中文字形</b>，拿它 showText 中文会直接抛
     * {@code IllegalArgumentException: No glyph for U+4E2D ...}。
     * 唯一正解是 {@code PDType0Font.load} <b>嵌入</b>一个真实的中文字体。
     *
     * <p><b>坑2</b>：{@code .ttc} 是「字体集合」（一个文件里多个字体），
     * 直接 {@code PDType0Font.load(doc, ttcFile)} 会失败或取到错误子字体，
     * 必须先用 {@link TrueTypeCollection} 按名字拆包。
     *
     * <p><b>坑3</b>：{@code embedSubset=true} 只嵌入用到的字形。
     * 中文字体全量嵌入会让 PDF 暴涨十几 MB；子集化后通常只有几十 KB。
     */
    public static PDType0Font loadCjkFont(PDDocument doc, File fontFile) throws IOException {
        String name = fontFile.getName().toLowerCase();
        if (name.endsWith(".ttc")) {
            TrueTypeCollection ttc = new TrueTypeCollection(fontFile);
            try {
                for (String fontName : PdfConstants.TTC_FONT_NAMES) {
                    TrueTypeFont ttf = ttc.getFontByName(fontName);
                    if (ttf != null) {
                        return PDType0Font.load(doc, ttf, true);
                    }
                }
                throw new IOException("TTC 集合里找不到任何已知中文字体：" + fontFile + "，尝试过的名字："
                        + String.join(",", PdfConstants.TTC_FONT_NAMES));
            } finally {
                ttc.close();
            }
        }
        // 普通 .ttf / .otf：直接嵌入（embedSubset=true）
        return PDType0Font.load(doc, fontFile);
    }

    // ---------------------------------------------------------------- 度量

    /**
     * 文本的实际宽度（pt）。
     *
     * <p><b>坑</b>：{@code font.getStringWidth} 返回的是「1/1000 文本空间单位」，
     * 不是 pt。必须 {@code / 1000 * fontSize} 才是页面坐标里的真实宽度。
     * 所有「居中 / 右对齐 / 自动换行」都依赖这个换算，算错就会溢出或错位。
     */
    public static float textWidth(PDFont font, float size, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * size;
    }

    /**
     * 按最大宽度把长文本拆成多行。
     *
     * <p><b>坑</b>：PDF 里没有「文本框自动换行」，必须自己按 {@link #textWidth} 逐字累加。
     * 中文可以逐字断行，但英文单词不能被从中间劈开——这里在超宽时回退到
     * 最后一个 ASCII 空格，避免把英文单词切断。
     */
    public static List<String> wrap(PDFont font, float size, String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (textWidth(font, size, cur.toString() + c) > maxWidth && cur.length() > 0) {
                int lastSpace = lastAsciiSpace(cur);
                if (lastSpace > 0) {
                    // 回退到单词边界，空格本身不保留
                    lines.add(cur.substring(0, lastSpace));
                    cur = new StringBuilder(cur.substring(lastSpace + 1));
                } else {
                    lines.add(cur.toString());
                    cur = new StringBuilder();
                }
            }
            cur.append(c);
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        return lines;
    }

    private static int lastAsciiSpace(StringBuilder sb) {
        for (int i = sb.length() - 1; i >= 0; i--) {
            if (sb.charAt(i) == ' ') {
                return i;
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------- 坐标与绘制

    /**
     * 页面可用内容宽度（页面宽 - 左右边距）。
     */
    public static float contentWidth(PDPage page) {
        PDRectangle box = page.getMediaBox();
        return box.getWidth() - 2 * PdfConstants.MARGIN_X;
    }

    /**
     * 新页的起始 Y（PDF 坐标原点在<b>左下角</b>，Y 轴向上）。
     *
     * <p><b>坑（新手第一跤）</b>：PDF 的坐标原点在左下、Y 向上，与 Swing/HTML（左上、Y 向下）
     * 完全相反。习惯性地「从上往下累减 Y」是对的，但一旦混用两套直觉，
     * 画出来的内容就会上下颠倒或跑到页面外——而 PDFBox 不会报错，只是「看不见」。
     */
    public static float startY(PDPage page) {
        return page.getMediaBox().getHeight() - PdfConstants.MARGIN_TOP;
    }

    /** 是否已经写到了页面底部（再写就越界）。 */
    public static boolean needNewPage(float y) {
        return y < PdfConstants.MARGIN_BOTTOM;
    }

    /**
     * 在 (x, y) 处画一行文本。
     *
     * <p><b>坑</b>：{@code setFont} 必须在 {@code beginText} 之后调用；
     * 且 {@code newLineAtOffset} 是<b>相对</b>当前位置的偏移，不是绝对坐标——
     * 连续画多行时不能反复当作绝对定位用。
     */
    public static void drawText(PDPageContentStream cs, PDFont font, float size, float x, float y, String text)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    /** 画一条直线（表格边框用）。 */
    public static void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    /** 画一个填充矩形（表头底色用）。 */
    public static void fillRect(PDPageContentStream cs, float x, float y, float w, float h, java.awt.Color color)
            throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }
}
