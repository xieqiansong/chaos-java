package lan.chaos.word.template;

import lan.chaos.word.common.constant.WordConstants;
import lan.chaos.word.common.model.Report;
import lan.chaos.word.common.model.ReportItem;
import lan.chaos.word.common.util.OutFiles;
import lan.chaos.word.common.util.WordKit;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 能力五：模板填充（占位符替换）——Word 专题里<b>最容易踩空、也最有教学价值</b>的能力。
 *
 * <p><b>WHY（痛点）</b>：Word 没有 Excel 那种 {@code {字段}} 的官方占位符引擎，
 * 模板填充本质是「在 XML 里找字符串替换」。但一个段落（Paragraph）由若干 <b>Run</b>（连续格式文本段）组成，
 * 文档一旦被人用 Word 编辑过，<b>${author} 极可能被拆成 "${" + "author" + "}" 三个 Run</b>。
 * 此时「逐 Run 替换」永远匹配不到完整占位符——这就是经典坑。
 *
 * <p><b>正确做法</b>：先 {@code paragraph.getText()} 拿整段文本做替换，
 * 再回写进第一个 Run、清掉其余 Run（合并 Run）。表格行、页眉页脚同理遍历替换即可。
 *
 * <p><b>关键 API</b>：{@code paragraph.getText()}（整段文本）、{@code run.setText}、
 * {@code XWPFTableRow} 行复制（{@code insertNewTableRow}）、{@code XWPFHeaderFooterPolicy} 页眉遍历。
 */
@Service
public class TemplateFillService {

    private final OutFiles outFiles;

    public TemplateFillService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File tpl = createTemplate();
        File naive = naiveFill(tpl);      // 错误示范：逐 Run 替换
        File correct = correctFill(tpl);  // 正确做法：合并 Run 后替换

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("模板 %s 含占位符 ${title}/${author}/${conclusion}/表格行/页眉${company}%n",
                tpl.getName()));
        sb.append(String.format("  朴素替换（逐 run）：author 段仍为「%s」（跨 run 割裂 → 失败）%n",
                readAuthor(naive)));
        sb.append(String.format("  正确替换（合并 run）：author 段已变为「%s」（成功）%n",
                readAuthor(correct)));
        return sb.toString();
    }

    /** 造一个含占位符的模板；其中 author 故意拆成 3 个 Run，复现「跨 run 割裂」现场。 */
    private File createTemplate() throws Exception {
        File file = outFiles.of("word-template.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream os = new FileOutputStream(file)) {

            // 标题占位符：单 Run（干净）
            doc.createParagraph().createRun().setText("${title}");

            // 作者占位符：故意拆成 3 个 Run（模拟 Word 编辑后的真实情况）
            XWPFParagraph authorP = doc.createParagraph();
            authorP.createRun().setText("${");
            authorP.createRun().setText("author");
            authorP.createRun().setText("}");

            // 结论占位符：单 Run
            doc.createParagraph().createRun().setText("${conclusion}");

            // 表格：表头 + 一行占位符（行复制用）
            XWPFTable table = doc.createTable(2, 3);
            WordKit.setCellText(table.getRow(0).getCell(0), "项目");
            WordKit.setCellText(table.getRow(0).getCell(1), "负责人");
            WordKit.setCellText(table.getRow(0).getCell(2), "状态");
            WordKit.setCellText(table.getRow(1).getCell(0), "${item.project}");
            WordKit.setCellText(table.getRow(1).getCell(1), "${item.owner}");
            WordKit.setCellText(table.getRow(1).getCell(2), "${item.status}");

            // 页眉占位符
            XWPFHeaderFooterPolicy policy = doc.getHeaderFooterPolicy();
            if (policy == null) {
                policy = doc.createHeaderFooterPolicy();
            }
            policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT)
                    .createParagraph().createRun().setText("${company}");

            doc.write(os);
        }
        return file;
    }

    /** 错误示范：逐 Run 替换。author 跨 3 个 Run，永远匹配不到 "${author}"。 */
    private File naiveFill(File tpl) throws Exception {
        File out = outFiles.of("word-template-naive.docx");
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(tpl));
             FileOutputStream os = new FileOutputStream(out)) {
            Report report = Report.sample();
            for (XWPFParagraph p : doc.getParagraphs()) {
                for (XWPFRun run : p.getRuns()) {
                    String t = run.getText(0);
                    if (t != null) {
                        t = t.replace("${title}", report.getTitle())
                                .replace("${author}", report.getAuthor())
                                .replace("${conclusion}", report.getConclusion());
                        // 注意：XWPFRun.setText(String) 在 5.5.1 是「追加」而非「替换」首节点，
                        // 若不指定 pos=0 会把文本重复一遍（${author} 变 ${${authorauthor}}），
                        // 反而把占位符本身破坏掉。这里用 setText(t, 0) 才是真正的「覆盖写」。
                        run.setText(t, 0);
                    }
                }
            }
            doc.write(os);
        }
        return out;
    }

    /** 正确做法：整段文本替换 + 合并 Run；表格行复制；页眉替换。 */
    private File correctFill(File tpl) throws Exception {
        File out = outFiles.of("word-template-filled.docx");
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(tpl));
             FileOutputStream os = new FileOutputStream(out)) {
            Report report = Report.sample();
            Map<String, String> values = new HashMap<>();
            values.put("${title}", report.getTitle());
            values.put("${author}", report.getAuthor());
            values.put("${conclusion}", report.getConclusion());
            values.put("${company}", "混沌研发部");

            for (XWPFParagraph p : doc.getParagraphs()) {
                replaceInParagraph(p, values);
            }
            if (!doc.getTables().isEmpty()) {
                fillTable(doc.getTables().get(0), report.getItems());
            }
            XWPFHeaderFooterPolicy policy = doc.getHeaderFooterPolicy();
            if (policy != null && policy.getDefaultHeader() != null) {
                for (XWPFParagraph p : policy.getDefaultHeader().getParagraphs()) {
                    replaceInParagraph(p, values);
                }
            }
            doc.write(os);
        }
        return out;
    }

    /** 合并 Run 替换：拿整段文本做替换，写回第一个 Run，清掉其余 Run。 */
    private static void replaceInParagraph(XWPFParagraph p, Map<String, String> values) {
        String text = p.getText();
        if (text == null) {
            return;
        }
        String replaced = text;
        for (Map.Entry<String, String> e : values.entrySet()) {
            replaced = replaced.replace(e.getKey(), e.getValue());
        }
        if (replaced.equals(text)) {
            return; // 本段无占位符，跳过
        }
        while (p.getRuns().size() > 0) {
            p.removeRun(0);
        }
        XWPFRun run = p.createRun();
        run.setText(replaced);
        WordKit.setCjkFont(run, WordConstants.CJK_FONT);
    }

    /** 表格行复制：模板行填第一条，其余向下插新行（本质 = Excel 填充）。 */
    private static void fillTable(XWPFTable table, List<ReportItem> items) {
        int tplIdx = -1;
        for (int r = 0; r < table.getRows().size(); r++) {
            if (table.getRow(r).getCell(0).getText().contains("${item.project}")) {
                tplIdx = r;
                break;
            }
        }
        if (tplIdx < 0) {
            return;
        }
        XWPFTableRow tplRow = table.getRow(tplIdx);
        int cols = tplRow.getTableCells().size();

        // 模板行填第一条
        for (int c = 0; c < cols; c++) {
            WordKit.setCellText(tplRow.getCell(c), resolve(tplRow.getCell(c).getText(), items.get(0)));
        }
        // 其余向下插新行（插在 tplIdx+1，顺序自然为正）
        for (int i = 1; i < items.size(); i++) {
            XWPFTableRow row = table.insertNewTableRow(tplIdx + 1);
            for (int c = 0; c < cols; c++) {
                WordKit.setCellText(row.createCell(), resolve(tplRow.getCell(c).getText(), items.get(i)));
            }
        }
    }

    private static String resolve(String placeholder, ReportItem item) {
        return placeholder
                .replace("${item.project}", item.getProject())
                .replace("${item.owner}", item.getOwner())
                .replace("${item.status}", item.getStatus());
    }

    /** 读回文档里含 "author" 的段落文本，用于验证替换是否生效。 */
    private static String readAuthor(File file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText();
                if (t != null && t.contains("author")) {
                    return t.trim();
                }
            }
        }
        return "(未找到)";
    }
}
