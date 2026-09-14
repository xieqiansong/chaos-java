package lan.chaos.excel.common.util;

import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.common.model.Order;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import java.util.List;

/** 写表的公共小工具：表头、数据行、可复用的样式。 */
public final class Rows {

    private Rows() {
    }

    /** 写表头到 sheet 的第 0 行（加粗）。 */
    public static void writeHeader(Workbook workbook, Sheet sheet, String[] headers) {
        writeHeader(workbook, sheet.createRow(0), headers);
    }

    /** 写表头到指定行（加粗）——表头不在首行时使用（如报表有标题行）。 */
    public static void writeHeader(Workbook workbook, Row row, String[] headers) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /** 建一张订单表并填满（表头 + rows 行数据）。导出横评里各方案共用它，保证对比的是同一份数据。 */
    public static Sheet fillOrders(Workbook workbook, int rows) {
        Sheet sheet = workbook.createSheet(ExcelConstants.SHEET_ORDERS);

        // SXSSF 的坑：流式 sheet 默认不跟踪列宽数据，直接 autoSizeColumn 会抛
        // "Could not auto-size column. Make sure the column was tracked prior to auto-sizing"。
        // 必须在写任何行之前先 track 需要自适应宽度的列。
        if (sheet instanceof SXSSFSheet) {
            SXSSFSheet streaming = (SXSSFSheet) sheet;
            for (int i = 0; i < ExcelConstants.HEADERS.length; i++) {
                streaming.trackColumnForAutoSizing(i);
            }
        }

        writeHeader(workbook, sheet, ExcelConstants.HEADERS);
        CellStyle moneyStyle = moneyStyle(workbook);
        CellStyle dateStyle = dateStyle(workbook);

        List<Order> data = Order.samples(rows);
        for (int i = 0; i < data.size(); i++) {
            Order order = data.get(i);
            Row row = sheet.createRow(i + 1);
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
        return sheet;
    }

    /** 金额列样式（保留 2 位小数）；每个 Workbook 只创建一次并复用——见 StyleWriteService 的 WHY。 */
    public static CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    /** 日期列样式。 */
    public static CellStyle dateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("yyyy-MM-dd HH:mm:ss"));
        return style;
    }

    /**
     * 自适应列宽（中文列宽要按字节算，POI 的 autoSizeColumn 在中文下会偏窄，故统一加余量）。
     *
     * <p>SXSSF 上还有个隐性差异：列宽是<b>只按仍在滑动窗口内的行</b>估算的，
     * 所以大文件导出的列宽可能只反映最后 N 行的数据。要么把窗口调大，
     * 要么干脆写死列宽——生产导出通常选择后者。
     */
    public static void autoWidth(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1024, 255 * 256));
        }
    }

    /** 生成表头常量对应的列数。 */
    public static int headerCount() {
        return ExcelConstants.HEADERS.length;
    }
}
