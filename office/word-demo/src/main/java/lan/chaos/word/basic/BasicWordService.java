package lan.chaos.word.basic;

import lan.chaos.word.common.constant.WordConstants;
import lan.chaos.word.common.model.Report;
import lan.chaos.word.common.model.ReportItem;
import lan.chaos.word.common.util.OutFiles;
import lan.chaos.word.common.util.WordKit;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

/**
 * 能力一：XWPF 写结构化 .docx（标题 / 正文 / 有序无序列表 / 表格 / 图片 / 页眉页脚 / 分页）。
 *
 * <p><b>WHY（痛点）</b>：Word 的 docx 本质是「一个 zip 包里的 XML」，POI 用 XWPF 这套 API 把 XML 封装成
 * 段落 / 表格 / Run（一段连续格式文本）。入门门槛比 Excel 高，因为「样式 / 字体 / 合并单元格」
 * 都得下钻到 XML Beans。本类先把最常用的骨架跑通。
 *
 * <p><b>关键 API</b>：{@code XWPFDocument}（文档）、{@code createParagraph/createRun}（段落与文本段）、
 * {@code createTable}（表格）、{@code addPicture}（图片）、{@code XWPFHeaderFooterPolicy}（页眉页脚）、
 * {@code run.addBreak(PAGE)}（分页）。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>原生没有「流式写」模型</b>：XWPFDocument 必须整篇在内存里构建，写盘是一次性的。
 *       这与 Excel 的 SXSSF 形成鲜明对比——Word 没有 SXSSF，超大文档（数万段）会直接吃内存，
 *       详见 bigdoc 能力。</li>
 *   <li><b>有序/无序列表别用字符串硬拼</b>：这里用「1. 」「• 」前缀演示视觉效果，
 *       真正可编辑的 Word 编号需要 {@code XWPFNumbering}（建 abstractNum + num），生产上建议封装成工具方法。</li>
 *   <li><b>页眉页脚必须先有 HeaderFooterPolicy</b>：{@code getHeaderFooterPolicy()} 可能为 null，
 *       要先 {@code createHeaderFooterPolicy()} 再 {@code createHeader}。</li>
 * </ol>
 */
@Service
public class BasicWordService {

    private final OutFiles outFiles;

    public BasicWordService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File file = build();
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            int paragraphs = doc.getParagraphs().size();
            int tables = doc.getTables().size();
            String header = readHeader(doc);
            String footer = readFooter(doc);
            return String.format("生成 %s（%s）%n  段落数=%d, 表格数=%d%n  页眉=%s%n  页脚=%s%n",
                    file.getName(), OutFiles.readableSize(file), paragraphs, tables, header, footer);
        }
    }

    public File build() throws Exception {
        File file = outFiles.of("word-basic.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream os = new FileOutputStream(file)) {

            // 标题
            XWPFParagraph title = doc.createParagraph();
            XWPFRun tr = title.createRun();
            tr.setText("项目周报");
            tr.setBold(true);
            tr.setFontSize(22);
            WordKit.setCjkFont(tr, WordConstants.CJK_TITLE_FONT);

            // 作者 / 日期（一行灰字）
            XWPFParagraph meta = doc.createParagraph();
            XWPFRun mr = meta.createRun();
            mr.setText("张三 · 研发一部 · 2026-06-30");
            mr.setFontSize(11);
            mr.setColor("808080");

            // 正文
            doc.createParagraph().createRun().setText("本季度共推进 3 个项目，整体进展顺利。以下分项说明。");

            // 有序列表（真实 Word 编号需 XWPFNumbering，较繁琐；此处用前缀演示视觉效果）
            doc.createParagraph().createRun().setText("1. 完成核心模块重构，性能提升约 30%。");
            doc.createParagraph().createRun().setText("2. 接入统一鉴权，覆盖 12 个接口。");
            doc.createParagraph().createRun().setText("3. 两个项目存在阻塞，需协调测试资源。");

            // 无序列表
            doc.createParagraph().createRun().setText("• 风险：测试环境不稳定");
            doc.createParagraph().createRun().setText("• 风险：第三方接口超时");

            // 表格
            XWPFTable table = doc.createTable(4, 3);
            String[] headers = {"项目", "负责人", "状态"};
            for (int c = 0; c < 3; c++) {
                WordKit.setCellText(table.getRow(0).getCell(c), headers[c]);
            }
            Report report = Report.sample();
            for (int r = 0; r < report.getItems().size(); r++) {
                XWPFTableRow row = table.getRow(r + 1);
                ReportItem item = report.getItems().get(r);
                WordKit.setCellText(row.getCell(0), item.getProject());
                WordKit.setCellText(row.getCell(1), item.getOwner());
                WordKit.setCellText(row.getCell(2), item.getStatus());
            }

            // 图片（程序生成 PNG，无需外部资源）
            XWPFRun picRun = doc.createParagraph().createRun();
            byte[] png = generatePng();
            try (InputStream is = new ByteArrayInputStream(png)) {
                picRun.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "demo.png",
                        WordConstants.IMAGE_WIDTH_EMU, WordConstants.IMAGE_HEIGHT_EMU);
            }

            // 分页 + 第二段
            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("—— 以下为详细分析 ——");
            WordKit.pageBreak(p2.getRuns().get(p2.getRuns().size() - 1));
            doc.createParagraph().createRun().setText("（此处省略 1 万字详细分析……）");

            // 页眉 / 页脚
            addHeaderFooter(doc);

            doc.write(os);
        }
        return file;
    }

    private void addHeaderFooter(XWPFDocument doc) {
        XWPFHeaderFooterPolicy policy = doc.getHeaderFooterPolicy();
        if (policy == null) {
            policy = doc.createHeaderFooterPolicy();
        }
        XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
        header.createParagraph().createRun().setText("机密 · 项目周报");

        XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
        footer.createParagraph().createRun().setText("仅供内部参考");
    }

    private static String readHeader(XWPFDocument doc) {
        XWPFHeaderFooterPolicy policy = doc.getHeaderFooterPolicy();
        if (policy == null || policy.getDefaultHeader() == null) {
            return "(无)";
        }
        return policy.getDefaultHeader().getParagraphs().stream()
                .map(XWPFParagraph::getText).collect(Collectors.joining(" "));
    }

    private static String readFooter(XWPFDocument doc) {
        XWPFHeaderFooterPolicy policy = doc.getHeaderFooterPolicy();
        if (policy == null || policy.getDefaultFooter() == null) {
            return "(无)";
        }
        return policy.getDefaultFooter().getParagraphs().stream()
                .map(XWPFParagraph::getText).collect(Collectors.joining(" "));
    }

    /** 用 AWT 画一张简单 PNG（红底白字），返回字节——避免依赖外部图片资源。 */
    private static byte[] generatePng() throws Exception {
        BufferedImage img = new BufferedImage(360, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 360, 200);
        g.setColor(Color.RED);
        g.fillRect(20, 20, 320, 160);
        g.setColor(Color.WHITE);
        g.drawString("DEMO CHART", 120, 110);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
