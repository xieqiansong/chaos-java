package lan.chaos.word.read;

import lan.chaos.word.common.util.OutFiles;
import lan.chaos.word.common.util.WordKit;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * 能力四：读取（XWPF 读 docx；.doc 用 HWPF 的局限见下方说明）。
 *
 * <p><b>WHY（痛点）</b>：写是「构造 XML」，读是「反向解析 XML」。docx 用 XWPF 遍历段落 / 表格即可；
 * 但老 .doc 是二进制 OLE2 格式，POI 用 HWPF 处理，<b>且 HWPF 在 POI 里长期处于「不完整」状态</b>——
 * 能凑合读纯文本，样式 / 表格 / 批注基本残缺。所以生产上 .doc 一律当作「待淘汰格式」处理。
 *
 * <p><b>关键 API</b>：{@code XWPFDocument.getParagraphs()/getTables()}（读 docx）。
 * 读 .doc 的 API 形态是 {@code new HWPFDocument(InputStream)} + {@code WordExtractor.getParagraphText()}，
 * 但因 POI 5.5.1 已移除 {@code HWPFDocument} 的无参构造、且 HWPF 写 .doc 残缺，本 Demo <b>不运行时生成 .doc</b>，
 * 仅以代码形态在上方注释说明，避免引入不可靠路径。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>.doc 读出来只有文本</b>：{@code WordExtractor} 拿到的是纯文本段落，
 *       样式 / 表格结构在 HWPF 下经常失真，别指望用它做精细解析。</li>
 *   <li><b>docx 段落可能为空</b>：遍历时要过滤空段落，否则统计行数会虚高。</li>
 *   <li><b>能写 .doc 但别写</b>：HWPF 写 .doc 功能残缺（本 POI 版本甚至无无参构造可用），
 *       新需求一律输出 .docx，.doc 只做「读取兼容」。</li>
 * </ol>
 */
@Service
public class ReadService {

    private final OutFiles outFiles;

    public ReadService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File docx = buildDocx();

        StringBuilder sb = new StringBuilder();
        try (XWPFDocument d = new XWPFDocument(new FileInputStream(docx))) {
            long paraCount = d.getParagraphs().stream().filter(p -> !p.getText().isEmpty()).count();
            long tableCount = d.getTables().size();
            String firstCell = d.getTables().isEmpty()
                    ? "(无表)" : d.getTables().get(0).getRow(0).getCell(0).getText();
            sb.append(String.format("读 docx %s：段落=%d, 表格=%d, 首格=%s%n",
                    docx.getName(), paraCount, tableCount, firstCell));
        }
        sb.append("  → .docx 用 XWPF 遍历段落/表格即可，干净可靠。\n");
        sb.append("  → 老 .doc(OLE2) 需 HWPF + WordExtractor，但该 API 在 POI 中长期残缺：\n");
        sb.append("    仅能可靠读纯文本，样式/表格常失真，且本 POI 版本 HWPFDocument 已无无参构造（写 .doc 不可用）。\n");
        sb.append("    新项目一律用 .docx；.doc 只做读取兼容，不在新链路里生成。\n");
        return sb.toString();
    }

    private File buildDocx() throws Exception {
        File file = outFiles.of("word-read.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream os = new FileOutputStream(file)) {
            doc.createParagraph().createRun().setText("这是要被读回的标题");
            doc.createParagraph().createRun().setText("这是正文第一段。");
            XWPFTable table = doc.createTable(2, 2);
            WordKit.setCellText(table.getRow(0).getCell(0), "姓名");
            WordKit.setCellText(table.getRow(0).getCell(1), "分数");
            WordKit.setCellText(table.getRow(1).getCell(0), "李四");
            WordKit.setCellText(table.getRow(1).getCell(1), "95");
            doc.write(os);
        }
        return file;
    }
}
