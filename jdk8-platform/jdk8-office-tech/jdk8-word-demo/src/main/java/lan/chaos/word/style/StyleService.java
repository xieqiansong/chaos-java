package lan.chaos.word.style;

import lan.chaos.word.common.util.OutFiles;
import lan.chaos.word.common.util.WordKit;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * 能力二：样式与中文字体（EastAsia 槽位坑）。
 *
 * <p><b>WHY（痛点）</b>：Word 文档里每个字符的字体分三个槽位——Ascii、HAnsi、EastAsia。
 * 英文字符走 Ascii/HAnsi，<b>中文字符走 EastAsia</b>。POI 的 {@code setFontFamily} 只填了 Ascii 槽位，
 * 中文因此回退成阅读器默认字体（常常是方块或难看的衬线体）。必须显式设置 EastAsia 才真正生效。
 * 段落样式（标题 1/2、正文）同理，新建文档没有任何样式定义，{@code setStyle} 只是写了个引用。
 *
 * <p><b>关键 API</b>：{@code XWPFStyles} + {@code CTStyle}（建样式）、
 * {@code run.getCTR().getRPr().getRFonts().setEastAsia(...)}（中文字体槽位）。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>中文字体必须 setEastAsia</b>：见 {@link WordKit#setCjkFont}。</li>
 *   <li><b>setStyle 前先 ensure 样式</b>：新建 {@code XWPFDocument} 没有样式定义，
 *       直接用 {@code setStyle("Heading1")} 在部分阅读器里不生效，需用 CTStyle 真正写进 styles.xml。</li>
 * </ol>
 */
@Service
public class StyleService {

    private final OutFiles outFiles;

    public StyleService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File file = build();
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("生成 %s（%s）%n", file.getName(), OutFiles.readableSize(file)));
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText();
                if (text.contains("标题") || text.contains("中文")) {
                    String shown = text.length() > 14 ? text.substring(0, 14) : text;
                    sb.append(String.format("  段落[%-14s] style=%-8s eastAsia=%s%n",
                            shown, p.getStyle(), readEastAsia(p)));
                }
            }
            sb.append("  → 只 setFontFamily 的段落 eastAsia 为空（中文回退默认字体），setCjkFont 的段落 eastAsia=宋体（生效）\n");
            return sb.toString();
        }
    }

    public File build() throws Exception {
        File file = outFiles.of("word-style.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream os = new FileOutputStream(file)) {

            // 先把样式真正写进文档
            WordKit.ensureParagraphStyle(doc, "Title", 44, "2E74B5");
            WordKit.ensureParagraphStyle(doc, "Heading1", 28, "2E74B5");

            XWPFParagraph title = doc.createParagraph();
            title.setStyle("Title");
            title.createRun().setText("这是标题（Title 样式）");

            XWPFParagraph h1 = doc.createParagraph();
            h1.setStyle("Heading1");
            h1.createRun().setText("这是一级标题（Heading1 样式）");

            // 坑演示 1：只设 Ascii 槽位（显式 FontCharRange.ASCII），EastAsia 槽位留空
            // 注意：POI 5.5.1 的 setFontFamily(String) 会把 Ascii/HAnsi/Cs/EastAsia 一起设了，
            // 所以「只调 setFontFamily 就留空 EastAsia」的坑在 5.5.1 已不复现；
            // 必须用 setFontFamily(name, FontCharRange.ASCII) 才能只填 Ascii，从而暴露 EastAsia 坑。
            XWPFParagraph p1 = doc.createParagraph();
            XWPFRun r1 = p1.createRun();
            r1.setText("中文只设 Ascii 字体：宋体（EastAsia 为空）");
            r1.setFontFamily("宋体", XWPFRun.FontCharRange.ascii);

            // 正确做法：同时设 EastAsia
            XWPFParagraph p2 = doc.createParagraph();
            XWPFRun r2 = p2.createRun();
            r2.setText("中文同时设 EastAsia 字体：宋体（生效）");
            WordKit.setCjkFont(r2, "宋体");

            doc.write(os);
        }
        return file;
    }

    /** 读回段落里第一个 run 的 EastAsia 字体（没设则显示「空」）。 */
    private static String readEastAsia(XWPFParagraph p) {
        for (XWPFRun run : p.getRuns()) {
            if (run.getCTR().isSetRPr()) {
                CTRPr rPr = run.getCTR().getRPr();
                // POI 5.5.1 的 CTRPr 里 rFonts 以数组形式暴露，用 sizeOfRFontsArray/getRFontsArray(0)
                if (rPr.sizeOfRFontsArray() > 0) {
                    String ea = rPr.getRFontsArray(0).getEastAsia();
                    if (ea != null) {
                        return ea;
                    }
                }
            }
        }
        return "(空)";
    }
}
