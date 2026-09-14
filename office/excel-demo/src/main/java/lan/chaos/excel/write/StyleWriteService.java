package lan.chaos.excel.write;

import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.common.model.Order;
import lan.chaos.excel.common.util.OutFiles;
import lan.chaos.excel.common.util.Rows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * 能力二：样式、合并单元格、公式、下拉、批注、冻结窗格。
 *
 * <p><b>WHY（痛点）</b>：业务要的从来不是"一堆数据"，而是"能直接交给老板看的报表"——
 * 标题、合并单元格、金额格式、合计公式、下拉选项，缺一样就得人工在 Excel 里再加工一次。
 *
 * <p><b>关键 API</b>：{@code CellStyle} / {@code Font} / {@code DataFormat}（数字格式）、
 * {@code sheet.addMergedRegion}（合并）、{@code cell.setCellFormula} + {@code XSSFFormulaEvaluator}
 * （公式与求值）、{@code DataValidationHelper}（下拉）、{@code createFreezePane}（冻结表头）。
 *
 * <p><b>生产坑（本类三条，条条见血）</b>：
 * <ol>
 *   <li><b>CellStyle 有总量上限（64000）</b>，且 <code>workbook.createCellStyle()</code>
 *       <u>不会复用</u>。在数据行里逐行 new 样式，1 万行就造 1 万个样式、10 万行直接抛异常。
 *       <b>正确做法：样式在循环外创建，循环内只 setCellStyle 复用。</b>见 {@link #demoCellStyleLimit()}。</li>
 *   <li><b>公式写完必须求值</b>，否则用 POI 读回来是 0（Excel 打开时才会计算）。
 *       导出给下游系统解析时尤其致命。</li>
 *   <li><b>autoSizeColumn 对中文偏窄</b>（它按字符数估算而非像素），需要额外补宽；
 *       且大表逐列 autoSize 很慢，通常只对前几列做。</li>
 * </ol>
 */
@Service
public class StyleWriteService {

    private final OutFiles outFiles;

    public StyleWriteService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    /** 生成一份"能直接看"的报表：标题合并 + 表头冻结 + 金额格式 + 合计公式 + 状态下拉 + 表头批注。 */
    public File writeReport(int rows) throws IOException {
        File file = outFiles.of("style-report.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream os = Files.newOutputStream(file.toPath())) {

            Sheet sheet = workbook.createSheet("报表");
            int columnCount = ExcelConstants.HEADERS.length;

            // 1) 标题行：跨列合并
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("订单报表（POI 样式演示）");
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));

            // 2) 表头（第 2 行，index=1）
            Row headerRow = sheet.createRow(1);
            Rows.writeHeader(workbook, headerRow, ExcelConstants.HEADERS);

            // 3) 数据行：样式在循环外创建好，循环内只复用（避免撞上 64000 上限）
            CellStyle moneyStyle = Rows.moneyStyle(workbook);
            CellStyle dateStyle = Rows.dateStyle(workbook);
            List<Order> data = Order.samples(rows);
            for (int i = 0; i < data.size(); i++) {
                Order order = data.get(i);
                Row row = sheet.createRow(i + 2); // 前两行是标题与表头
                row.createCell(0).setCellValue(order.getOrderNo());
                row.createCell(1).setCellValue(order.getCustomer());
                row.createCell(2).setCellValue(order.getProduct());
                row.createCell(3).setCellValue(order.getQuantity());
                Cell amount = row.createCell(4);
                amount.setCellValue(order.getAmount().doubleValue());
                amount.setCellStyle(moneyStyle);
                row.createCell(5).setCellValue(order.getStatus());
                Cell createdAt = row.createCell(6);
                createdAt.setCellValue(order.getCreatedAt());
                createdAt.setCellStyle(dateStyle);
            }

            // 4) 合计行 + SUM 公式（数据区 Excel 行号 3 .. rows+2）
            int lastDataExcelRow = rows + 2;
            Row totalRow = sheet.createRow(rows + 2);
            totalRow.createCell(3).setCellValue("合计");
            Cell sumCell = totalRow.createCell(4);
            sumCell.setCellFormula(String.format("SUM(E3:E%d)", lastDataExcelRow));
            sumCell.setCellStyle(moneyStyle);

            // 5) 下拉：状态列只允许 PAID / CANCELLED
            DataValidationHelper validationHelper = sheet.getDataValidationHelper();
            DataValidationConstraint constraint =
                    validationHelper.createExplicitListConstraint(new String[]{"PAID", "CANCELLED"});
            CellRangeAddressList statusRegion = new CellRangeAddressList(2, lastDataExcelRow - 1, 5, 5);
            DataValidation validation = validationHelper.createValidation(constraint, statusRegion);
            sheet.addValidationData(validation);

            // 6) 批注：给"金额"表头加说明
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(4);
            anchor.setRow1(1);
            anchor.setCol2(7);
            anchor.setRow2(4);
            Comment comment = drawing.createCellComment(anchor);
            comment.setString(workbook.getCreationHelper()
                    .createRichTextString("金额单位：元；由 SUM 公式自动合计"));
            headerRow.getCell(4).setCellComment(comment);

            // 7) 冻结前两行，滚动时标题与表头保持可见
            sheet.createFreezePane(0, 2);

            // 8) 关键：公式必须主动求值，否则 POI 读回来是 0（Excel 打开时才会算）
            XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);

            Rows.autoWidth(sheet, columnCount);
            workbook.write(os);
        }
        return file;
    }

    /** 回读合计单元格的数值（验证公式已被求值——未求值时这里是 0）。 */
    public double readTotal(File file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row totalRow = sheet.getRow(sheet.getLastRowNum());
            return totalRow.getCell(4).getNumericCellValue();
        }
    }

    /**
     * 演示 CellStyle 的 64000 上限：逐行新建样式 = 拿行数去撞这个上限。
     * 这是"导出 10 万行突然报错"最常见的原因，且报错信息完全不提"你建了太多样式"。
     */
    public String demoCellStyleLimit() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            int created = 0;
            try {
                for (int i = 0; i < ExcelConstants.CELL_STYLE_LIMIT + 10; i++) {
                    workbook.createCellStyle();
                    created++;
                }
                return "连续创建 " + created + " 个 CellStyle -> 未触发上限（与预期不符）";
            } catch (Exception e) {
                return String.format("连续创建到第 %d 个 CellStyle -> %s: %s",
                        created, e.getClass().getSimpleName(), e.getMessage());
            }
        } catch (IOException e) {
            return "演示失败：" + e.getMessage();
        }
    }

    /** 控制台 / 测试统一入口。 */
    public String run() throws IOException {
        int rows = ExcelConstants.DEFAULT_ROWS;
        File file = writeReport(rows);
        return String.format("生成报表 %d 行 -> %s, 合计(公式求值后)=%.2f%n坑3 %s%n",
                rows, OutFiles.readableSize(file), readTotal(file), demoCellStyleLimit());
    }
}
