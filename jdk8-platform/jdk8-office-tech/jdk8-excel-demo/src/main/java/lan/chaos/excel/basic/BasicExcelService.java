package lan.chaos.excel.basic;

import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.common.util.OutFiles;
import lan.chaos.excel.common.util.Rows;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * 能力一：POI 三剑客 <b>HSSF / XSSF / SXSSF</b> 的取舍。
 *
 * <p><b>WHY（痛点）</b>：选错 Workbook 实现，轻则内存翻几倍、重则直接 OOM 或文件打不开——
 * 而三者的 API 几乎一样（都实现 {@link Workbook}），光看代码根本看不出差别，必须理解它们的存储模型。
 *
 * <p><b>关键 API 与选型</b>：
 * <ul>
 *   <li>{@code HSSFWorkbook} → .xls（Excel 97-2003，OLE2 二进制）。上限 65536 行 × 256 列，
 *       全内存。<b>只用于兼容老系统</b>，新项目一律不用。</li>
 *   <li>{@code XSSFWorkbook} → .xlsx（OOXML）。上限 104 万行，<b>全内存</b>：
 *       10 万行 × 20 列实测能吃到 1GB+ 堆。适合小报表（几千行内）。</li>
 *   <li>{@code SXSSFWorkbook} → .xlsx 的<b>流式</b>实现：只保留滑动窗口内的行在内存，
 *       其余刷到磁盘临时文件。内存恒定（≈ 窗口大小），是<b>大数据导出的唯一正解</b>。</li>
 * </ul>
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li>SXSSF 的行一旦刷出窗口就<b>再也取不回来</b>（{@code sheet.getRow(i)} 返回 null），
 *       所以不能在导出中途回头改样式——样式必须<b>边写边定</b>。</li>
 *   <li>SXSSF 会在磁盘留临时文件，必须 {@code wb.dispose()} 清理，否则临时目录会被撑爆。</li>
 *   <li>三者的读统一走 {@code WorkbookFactory.create(file)}，但<b>不要</b>用它读超大文件
 *       （那是全内存模型，见 read/SaxReadService）。</li>
 * </ol>
 */
@Service
public class BasicExcelService {

    private final OutFiles outFiles;

    public BasicExcelService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    /** HSSF：写 .xls（Excel 97-2003）。超过 65536 行会抛异常——这是格式硬上限，不是 POI 的限制。 */
    public File writeXls(int rows) throws IOException {
        if (rows > ExcelConstants.HSSF_MAX_ROWS) {
            throw new IllegalArgumentException(
                    "HSSF(.xls) 上限 " + ExcelConstants.HSSF_MAX_ROWS + " 行，当前请求 " + rows + " 行；请改用 .xlsx");
        }
        File file = outFiles.of("basic-orders.xls");
        try (Workbook workbook = new HSSFWorkbook();
             OutputStream os = Files.newOutputStream(file.toPath())) {
            fill(workbook, rows);
            workbook.write(os);
        }
        return file;
    }

    /** XSSF：写 .xlsx，全内存模型。行数无压力，但内存与行数成正比。 */
    public File writeXlsx(int rows) throws IOException {
        File file = outFiles.of("basic-orders.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream os = Files.newOutputStream(file.toPath())) {
            fill(workbook, rows);
            workbook.write(os);
        }
        return file;
    }

    /**
     * SXSSF：写 .xlsx，流式模型。
     *
     * @param windowSize 滑动窗口大小：内存中最多保留这么多行，超出的刷到磁盘。
     *                   越大越快但越吃内存；POI 默认 100，导出大文件常设 500~2000。
     */
    public File writeSxssf(int rows, int windowSize) throws IOException {
        File file = outFiles.of("basic-orders-sxssf.xlsx");
        SXSSFWorkbook workbook = new SXSSFWorkbook(windowSize);
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            fill(workbook, rows);
            workbook.write(os);
        } finally {
            // 关键：清理 SXSSF 刷到磁盘的临时文件，否则临时目录会被撑爆
            workbook.dispose();
        }
        return file;
    }

    /** 回读：统计有效数据行数（跳过表头）。WorkbookFactory 自动按扩展名选择 HSSF/XSSF 实现。 */
    public int readRowCount(File file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            return sheet.getLastRowNum(); // 0 是表头，所以 lastRowNum 即数据行数
        }
    }

    /**
     * 演示 SXSSF 的"已刷出行不可访问"：窗口外的行 {@code getRow()} 返回 null。
     * 这是"导出中途回头改样式"需求失败的根因。
     */
    public String demoFlushedRowsAreGone() throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook(50);
        try {
            Sheet sheet = workbook.createSheet("demo");
            for (int i = 0; i < 200; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("row-" + i);
            }
            Row flushed = sheet.getRow(10);   // 已刷出窗口 → null
            Row alive = sheet.getRow(199);    // 仍在窗口内 → 有值
            return String.format("窗口=50 共写 200 行 -> getRow(10)=%s, getRow(199)=%s",
                    flushed == null ? "null(已刷出，取不回来)" : flushed.getCell(0).getStringCellValue(),
                    alive == null ? "null" : alive.getCell(0).getStringCellValue());
        } finally {
            workbook.dispose();
        }
    }

    /** 演示 HSSF 的行数上限：超过 65536 行直接失败（.xls 格式的硬限制）。 */
    public String demoHssfRowLimit() {
        int overflow = ExcelConstants.HSSF_MAX_ROWS + 1;
        try {
            writeXls(overflow);
            return "写 " + overflow + " 行到 .xls -> 竟然成功了（不符合预期）";
        } catch (Exception e) {
            return "写 " + overflow + " 行到 .xls -> " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** 控制台 / 测试统一入口：返回「输入→输出」可读结果。 */
    public String run() throws IOException {
        StringBuilder sb = new StringBuilder();
        int rows = ExcelConstants.DEFAULT_ROWS;

        File xls = writeXls(rows);
        sb.append(String.format("HSSF  .xls  写 %d 行 -> %s, 回读 %d 行%n",
                rows, OutFiles.readableSize(xls), readRowCount(xls)));

        File xlsx = writeXlsx(rows);
        sb.append(String.format("XSSF  .xlsx 写 %d 行 -> %s, 回读 %d 行%n",
                rows, OutFiles.readableSize(xlsx), readRowCount(xlsx)));

        File sxlsx = writeSxssf(rows, ExcelConstants.SXSSF_DEFAULT_WINDOW);
        sb.append(String.format("SXSSF .xlsx 写 %d 行 -> %s, 回读 %d 行%n",
                rows, OutFiles.readableSize(sxlsx), readRowCount(sxlsx)));

        sb.append("坑1 ").append(demoHssfRowLimit()).append('\n');
        sb.append("坑2 ").append(demoFlushedRowsAreGone()).append('\n');
        return sb.toString();
    }

    /** 填充（三类 Workbook 共用，体现 SS 通用接口的价值）。 */
    private void fill(Workbook workbook, int rows) {
        Sheet sheet = Rows.fillOrders(workbook, rows);
        Rows.autoWidth(sheet, ExcelConstants.HEADERS.length);
    }
}
