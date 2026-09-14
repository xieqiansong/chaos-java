package lan.chaos.pdf.basic;

import lan.chaos.pdf.common.constant.PdfConstants;
import lan.chaos.pdf.common.model.Statement;
import lan.chaos.pdf.common.util.OutFiles;
import lan.chaos.pdf.common.util.PdfCanvas;
import lan.chaos.pdf.common.util.PdfKit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 能力二：结构化文档绘制——把「标题 / 正文 / 列表 / 图片 / 多页」画出来。
 *
 * <p><b>WHY（痛点）</b>：PDF 与 Word/HTML 的根本差异是——<b>PDF 没有布局引擎</b>。
 * 没有段落、没有自动换行、没有自动分页。每一行的坐标、每一段的折行、每一页的切换，
 * 全都要程序自己算。这是 PDF 导出代码普遍比 Word 导出长 3~5 倍的原因。
 *
 * <p><b>关键 API</b>：{@code PDDocument / PDPage / PDPageContentStream} +
 * {@code PDType0Font} + {@code LosslessFactory.createFromImage}。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>坐标系原点在左下角、Y 轴向上</b>：与 Swing/HTML（左上、Y 向下）完全相反。
 *       混用两套直觉的结果是内容上下颠倒或跑到页面外——PDFBox <b>不报错</b>，只是看不见。</li>
 *   <li><b>必须自己分页</b>：Y 一路累减到负数也不会自动换页，内容直接画到页面外丢失。</li>
 *   <li><b>内容流必须 close()</b>：未关闭的 {@code PDPageContentStream} 不会写入页面，
 *       症状是「文件能生成但打开是空白」。</li>
 *   <li><b>文本宽度要自己量</b>：居中/右对齐/换行都依赖
 *       {@code font.getStringWidth(text) / 1000 * fontSize}。</li>
 * </ol>
 */
@Service
public class BasicPdfService {

    private final OutFiles outFiles;

    public BasicPdfService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File fontFile = PdfKit.findCjkFontFile();
        if (fontFile == null) {
            return "  跳过：未找到中文字体，无法演示文本绘制。";
        }
        File file = build(fontFile);
        int pages;
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(file)) {
            pages = doc.getNumberOfPages();
        }
        return String.format("  生成 %s（%s，%d 页）%n"
                        + "  演示：居中标题 / 自动换行正文 / 有序+无序列表 / 程序生成图片 / 跨页自动分页 / 页脚页码%n"
                        + "  坐标系：原点在左下角、Y 轴向上（与 HTML 相反），所有 Y 都从「页高-上边距」往下累减%n",
                file.getName(), OutFiles.readableSize(file), pages);
    }

    public File build(File fontFile) throws Exception {
        File out = outFiles.of("pdf-basic.pdf");
        Statement statement = Statement.sample();

        try (PDDocument doc = new PDDocument()) {
            PDType0Font cjk = PdfKit.loadCjkFont(doc, fontFile);

            try (PdfCanvas c = new PdfCanvas(doc)) {
                // ① 居中标题：必须用 getStringWidth 手动算居中起点
                String title = statement.getTitle();
                float titleW = PdfKit.textWidth(cjk, PdfConstants.TITLE_SIZE, title);
                float pageW = c.page().getMediaBox().getWidth();
                PdfKit.drawText(c.cs(), cjk, PdfConstants.TITLE_SIZE, (pageW - titleW) / 2, c.y(), title);
                c.gap(26f);

                // ② 元信息
                c.text(cjk, PdfConstants.BODY_SIZE, "客户：" + statement.getCustomer());
                c.text(cjk, PdfConstants.BODY_SIZE, "账期：" + statement.getPeriod());
                c.gap(8f);
                c.hr();

                // ③ 正文段落（长文本 → 自动换行）
                c.paragraph(cjk, PdfConstants.BODY_SIZE,
                        "这是一段用于演示自动换行的正文。PDF 里没有「文本框」这个概念，"
                                + "所以下面这行很长很长的文字必须由程序按字体宽度逐字测量后手动折行，"
                                + "否则它会一路画到页面外——而 PDFBox 不会给出任何错误提示。"
                                + "这一段还故意混入 English words，用来验证换行不会把英文单词从中间劈开。");
                c.gap(10f);

                // ④ 有序列表
                c.text(cjk, PdfConstants.HEADING_SIZE, "一、交付内容");
                for (int i = 1; i <= 3; i++) {
                    c.textAt(cjk, PdfConstants.BODY_SIZE, i + ". 交付物-" + i + "：已完成并通过验收", 20f, 18f);
                }
                c.gap(6f);

                // ⑤ 无序列表
                //    坑中坑：别用 "•"(U+2022) 这类装饰符号——中文字体（如 simhei）未必收录它，
                //    showText 会抛 "could not find the glyphId for the character"。
                //    要画特殊符号，先确认目标字体覆盖该码点，或直接换用字体一定有的 ASCII 字符。
                c.text(cjk, PdfConstants.HEADING_SIZE, "二、注意事项");
                for (String s : new String[]{"金额单位为「分」，避免浮点误差", "字体需随部署环境提供", "内容流用完必须 close"}) {
                    c.textAt(cjk, PdfConstants.BODY_SIZE, "- " + s, 20f, 18f);
                }
                c.gap(10f);

                // ⑥ 程序生成图片（柱状图）
                c.text(cjk, PdfConstants.HEADING_SIZE, "三、金额分布");
                BufferedImage chart = drawBarChart(statement);
                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, chart);
                float imgW = 320f;
                float imgH = imgW * chart.getHeight() / chart.getWidth();
                c.ensure(imgH + 10);
                c.cs().drawImage(pdImage, PdfConstants.MARGIN_X, c.y() - imgH, imgW, imgH);
                c.gap(imgH + 10);

                // ⑦ 自动分页演示：写足够多行，逼出「跨页」
                //    这里每一行都由 PdfCanvas#text 内部的 ensure() 判断是否该换页，
                //    换页时会自动 close 旧内容流、new 新页面、重置 Y。
                c.text(cjk, PdfConstants.HEADING_SIZE, "四、自动分页演示");
                for (int i = 1; i <= 40; i++) {
                    c.text(cjk, PdfConstants.BODY_SIZE,
                            String.format("第 %02d 行：若剩余高度不足，PdfCanvas 会自动换页并重置 Y 坐标。", i));
                }
            }

            // ⑦ 页脚页码：内容写完后逐页以「追加模式」补画
            addFooters(doc, cjk);
            doc.save(out);
        }
        return out;
    }

    /** 逐页追加页码。注意用 AppendMode.APPEND——否则会覆盖掉页面已有内容。 */
    private void addFooters(PDDocument doc, PDFont font) throws Exception {
        int total = doc.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = doc.getPage(i);
            String footer = "第 " + (i + 1) + " / " + total + " 页";
            float w = PdfKit.textWidth(font, 9f, footer);
            float x = (page.getMediaBox().getWidth() - w) / 2;
            try (PDPageContentStream cs =
                         new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                PdfKit.drawText(cs, font, 9f, x, 32f, footer);
            }
        }
    }

    /** 程序画一张柱状图（不依赖任何外部图片文件，保证 Demo 自包含）。 */
    private BufferedImage drawBarChart(Statement statement) {
        int w = 480;
        int h = 200;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        long max = 1;
        for (lan.chaos.pdf.common.model.StatementItem item : statement.getItems()) {
            max = Math.max(max, item.getAmountCents());
        }
        int n = statement.getItems().size();
        int barW = (w - 60) / n;
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < n; i++) {
            long v = statement.getItems().get(i).getAmountCents();
            int barH = (int) (140.0 * v / max);
            int x = 40 + i * barW;
            int y = h - 30 - barH;
            g.setColor(new Color(46, 116, 181));
            g.fillRect(x + 6, y, barW - 16, barH);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.valueOf(i + 1), x + barW / 2 - 3, h - 12);
        }
        g.setColor(Color.GRAY);
        g.drawLine(30, h - 30, w - 20, h - 30);
        g.dispose();
        return img;
    }
}
