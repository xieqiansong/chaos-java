package lan.chaos.pdf.font;

import lan.chaos.pdf.common.constant.PdfConstants;
import lan.chaos.pdf.common.util.OutFiles;
import lan.chaos.pdf.common.util.PdfKit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;

/**
 * 能力一：中文字体嵌入——<b>PDF 专题里最经典、也最容易当场翻车的坑</b>。
 *
 * <p><b>WHY（痛点）</b>：PDF 规范里的「标准 14 字体」（Helvetica / Times / Courier …）
 * 只覆盖拉丁字符，<b>没有任何中文字形</b>。拿它写中文，PDFBox 会在 {@code showText} 处直接抛：
 * <pre>IllegalArgumentException: U+4E2D ('.notdef') is not available in the font Helvetica, encoding: WinAnsiEncoding</pre>
 * 这与 Word/HTML 的体验完全不同（那里最多是显示成方块，不会抛异常）。
 * 唯一正解是 {@code PDType0Font.load(...)} <b>嵌入</b>一个真实中文字体。
 *
 * <p>注意异常文案随版本变化：PDFBox 2.x 是 {@code No glyph for U+4E2D ...}，
 * 3.0.x 改成了 {@code ... is not available in the font ...}。断言时应当匹配
 * <b>码点 U+4E2D</b> 这种稳定信息，而不是整句文案。
 *
 * <p><b>关键 API</b>：
 * <ul>
 *   <li>PDFBox 3.0 起标准字体写法变成 {@code new PDType1Font(Standard14Fonts.FontName.HELVETICA)}——
 *       2.0 时代的 {@code PDType1Font.HELVETICA} 静态常量已被移除，照抄旧博客会编译不过。</li>
 *   <li>普通字体文件：{@code PDType0Font.load(doc, ttfFile)}。</li>
 *   <li>{@code .ttc} 集合字体：必须先用 {@code TrueTypeCollection#getFontByName} 拆包
 *       （见 {@link PdfKit#loadCjkFont}）。</li>
 * </ul>
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>字体不能随包带</b>：中文字体 10~20 MB 且授权各异，正确做法是当作部署物，代码只探测。</li>
 *   <li><b>全量嵌入会让 PDF 暴涨</b>：子集化（只嵌入用到的字形）后通常从十几 MB 降到几十 KB。</li>
 *   <li><b>没有字体就没法写中文</b>：容器镜像里漏装字体是线上最常见事故，必须有明确失败提示。</li>
 * </ol>
 */
@Service
public class FontService {

    private final OutFiles outFiles;

    public FontService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File fontFile = PdfKit.findCjkFontFile();
        StringBuilder sb = new StringBuilder();

        sb.append("  探测到的中文字体：")
                .append(fontFile == null ? "（未找到，本环境无法演示中文字体）" : fontFile.getAbsolutePath())
                .append('\n');

        // 坑演示：标准 14 字体写中文 → 直接抛异常
        sb.append("  标准14字体(Helvetica)写中文：").append(naiveStandardFont()).append('\n');

        if (fontFile == null) {
            sb.append("  跳过中文写入演示：请安装中文字体（如 fonts-noto-cjk）后重试。\n");
            return sb.toString();
        }

        File embedded = writeWithCjkFont(fontFile);
        sb.append("  嵌入中文字体后：")
                .append(embedded.getName()).append("（").append(OutFiles.readableSize(embedded)).append("）\n");
        sb.append("  字体类型：").append(fontFile.getName().toLowerCase().endsWith(".ttc") ? "TTC 集合（已拆包）" : "TTF/OTF")
                .append('\n');
        return sb.toString();
    }

    /**
     * 错误示范：用标准 14 字体写中文。
     *
     * <p>注意 PDFBox 3.0 的写法变化——2.0 的 {@code PDType1Font.HELVETICA} 常量已不存在。
     */
    private String naiveStandardFont() {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDFont helvetica = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(helvetica, PdfConstants.BODY_SIZE);
                cs.newLineAtOffset(PdfConstants.MARGIN_X, PdfKit.startY(page));
                cs.showText("中文标题"); // ← 这里必然抛异常
                cs.endText();
            }
            return "竟然没有报错（说明该 PDFBox 版本对缺字形做了兜底）";
        } catch (Exception e) {
            // 预期：IllegalArgumentException: No glyph for U+4E2D ...
            String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
            return "失败（预期内）→ " + msg;
        }
    }

    /** 正确做法：嵌入中文字体后写中文（并画一条色块，验证中文宽度测量也正确）。 */
    private File writeWithCjkFont(File fontFile) throws Exception {
        File out = outFiles.of("pdf-font-cjk.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType0Font cjk = PdfKit.loadCjkFont(doc, fontFile);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PdfKit.startY(page);

                PdfKit.drawText(cs, cjk, PdfConstants.TITLE_SIZE, PdfConstants.MARGIN_X, y, "中文字体嵌入验证");
                y -= 30;

                PdfKit.drawText(cs, cjk, PdfConstants.BODY_SIZE, PdfConstants.MARGIN_X, y,
                        "这一行是中文，由 PDType0Font 嵌入字体后绘制，Helvetica 是画不出来的。");
                y -= 22;

                // 用文本宽度测量画一条下划线，验证 getStringWidth 对中文同样有效
                String sample = "中文宽度测量：下划线长度应与文字等宽";
                PdfKit.drawText(cs, cjk, PdfConstants.BODY_SIZE, PdfConstants.MARGIN_X, y, sample);
                float w = PdfKit.textWidth(cjk, PdfConstants.BODY_SIZE, sample);
                cs.setStrokingColor(Color.DARK_GRAY);
                cs.setLineWidth(0.8f);
                PdfKit.drawLine(cs, PdfConstants.MARGIN_X, y - 3, PdfConstants.MARGIN_X + w, y - 3);
            }
            doc.save(out);
        }
        return out;
    }
}
