package lan.chaos.word.table;

import lan.chaos.word.common.util.OutFiles;
import lan.chaos.word.common.util.WordKit;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * 能力三：复杂表格（合并单元格 / 列宽 / 背景色 / 边框）。
 *
 * <p><b>WHY（痛点）</b>：Word 表格比 Excel 脆弱得多——合并单元格不是一行 API，
 * 而是给每个被合并格手工打上 {@code w:hMerge}/{@code w:vMerge} 标记（起始格 RESTART，后续 CONTINUE）。
 * 漏打任何一个标记，文档打开就是错位的。背景色、列宽也都要下钻到 CT* 类。
 *
 * <p><b>关键 API</b>：{@code doc.createTable(rows, cols)}、{@code XWPFTableRow.getCell}、
 * {@code cell.getCTTc().getTcPr().addNewHMerge/VMerge}（合并）、
 * {@code CTShd.setFill}（背景）、{@code table.setBorderXxx}（边框）、{@code cell.setWidth}（列宽）。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>合并是「打标记」不是「删列」</b>：横向跨 3 列，要给 3 个格都打标记
 *       （首格 RESTART，其余 CONTINUE），只标记首格渲染会错位。</li>
 *   <li><b>纵向合并同理</b>：跨 N 行要给 N 个格打 vMerge 标记，被合并格内容留空。</li>
 *   <li><b>背景色别用 setColor 的坑</b>：{@code cell.setColor} 设的是边框色语义易混，
 *       背景底纹要走 {@code CTShd.setFill}（见本类的 setShading）。</li>
 * </ol>
 */
@Service
public class TableService {

    private final OutFiles outFiles;

    public TableService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run() throws Exception {
        File file = build();
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
            XWPFTable table = doc.getTables().get(0);
            int rows = table.getRows().size();
            int cols = table.getRow(0).getTableCells().size();
            String a1 = table.getRow(0).getCell(0).getCTTc().getTcPr().getHMerge().getVal().toString();
            String a2 = table.getRow(1).getCell(0).getCTTc().getTcPr().getVMerge().getVal().toString();
            return String.format("生成 %s（%s）%n  表格行数=%d, 列数=%d%n"
                            + "  A1 横向合并=%s（应为 restart），A2 纵向合并=%s（应为 restart）%n",
                    file.getName(), OutFiles.readableSize(file), rows, cols, a1, a2);
        }
    }

    public File build() throws Exception {
        File file = outFiles.of("word-table.docx");
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream os = new FileOutputStream(file)) {
            XWPFTable table = doc.createTable(4, 3);
            // setTopBorder(type, size, space, color) —— POI 没有 setBorderTop 这种单数命名
            table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");

            // 第 1 行：标题跨 3 列（3 个格都打横向合并标记）
            WordKit.setCellText(table.getRow(0).getCell(0), "季度汇总（跨 3 列）");
            WordKit.mergeHorizontal(table.getRow(0).getCell(0), "restart");
            WordKit.mergeHorizontal(table.getRow(0).getCell(1), "continue");
            WordKit.mergeHorizontal(table.getRow(0).getCell(2), "continue");
            setShading(table.getRow(0).getCell(0), "2E74B5");

            // 第 2、3 行：A 列纵向跨 2 行
            WordKit.setCellText(table.getRow(1).getCell(0), "项目进展");
            WordKit.mergeVertical(table.getRow(1).getCell(0), "restart");
            WordKit.setCellText(table.getRow(2).getCell(0), ""); // 被合并，内容留空
            WordKit.mergeVertical(table.getRow(2).getCell(0), "continue");
            setShading(table.getRow(1).getCell(0), "DDEBF7");

            WordKit.setCellText(table.getRow(1).getCell(1), "核心模块");
            WordKit.setCellText(table.getRow(1).getCell(2), "已完成");
            WordKit.setCellText(table.getRow(2).getCell(1), "统一鉴权");
            WordKit.setCellText(table.getRow(2).getCell(2), "进行中");

            // 第 4 行普通
            WordKit.setCellText(table.getRow(3).getCell(0), "说明");
            WordKit.setCellText(table.getRow(3).getCell(1), "整体可控");
            WordKit.setCellText(table.getRow(3).getCell(2), "—");

            // 列宽 + 整表宽
            table.getRow(0).getCell(0).setWidth("4000");
            table.setWidth(8000);

            doc.write(os);
        }
        return file;
    }

    /** 给单元格设背景底纹（fill 为 6 位十六进制 RGB）。 */
    private static void setShading(XWPFTableCell cell, String fill) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(fill);
    }
}
