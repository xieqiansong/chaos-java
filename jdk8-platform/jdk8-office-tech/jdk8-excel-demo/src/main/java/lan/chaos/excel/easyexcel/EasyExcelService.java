package lan.chaos.excel.easyexcel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.common.model.Order;
import lan.chaos.excel.common.util.OutFiles;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 能力四：EasyExcel（阿里，POI 之上的流式封装）。
 *
 * <p><b>WHY（痛点）</b>：原生 POI 写导出要自己管样式、管行、管 SXSSF 窗口、管 dispose，
 * 代码又长又容易漏。EasyExcel 把这些收成"一行代码"：
 * {@code EasyExcel.write(file, Order.class).sheet("订单").doWrite(list)}，
 * 读则用监听器<b>逐批回调</b>，内存恒定——这是它在国内成为事实标准的原因。
 *
 * <p><b>关键 API</b>：{@code @ExcelProperty}（表头映射）、{@code EasyExcel.write/read}、
 * {@code PageReadListener}（分批读）、{@code excelType(ExcelTypeEnum.CSV)}（切 CSV）。
 * 模板填充（{@code withTemplate().fill()}）在本 Demo 里<b>改由 POI 原生实现</b>，原因见下。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>监听器里绝对不能写数据库</b>：一次 invoke 一条就一次 DB 往返，10 万行 = 10 万次插入。
 *       必须用 {@code PageReadListener} 攒批（默认 100 条/批）后<b>批量入库</b>。</li>
 *   <li><b>EasyExcel 底层还是 POI</b>：它只是把 SXSSF 封装得更好，
 *       版本不兼容时报错会落在 POI 内部（本模块已用版本矩阵测试钉死 POI 5.5.1）。</li>
 *   <li><b>模板填充改用 POI 原生</b>：在 POI 5.5.1 + EasyExcel 4.0.3 组合下，
 *       {@code withTemplate().fill()} 的列表填充（即便加 {@code forceNewRow(true)}）<b>不会展开</b>，
 *       只会留一行空数据——它对本 Demo 这种"POI 程序化生成的模板"存在识别盲区。
 *       于是这里用 POI 直接做"复制占位符行 + 字符串替换"，逻辑零黑盒，
 *       也正好揭示了模板填充的本质。<b>若模板是 Excel 手工画的 .xlsx，EasyExcel 的 fill 通常可用</b>。</li>
 *   <li><b>@ExcelProperty 是 EasyExcel 的注解</b>，与 Hutool 的 {@code @Alias} 不互通——
 *       同一个 model 被两套注解同时污染是常见混乱源，见 hutool 包的说明。</li>
 * </ol>
 */
@Service
public class EasyExcelService {

    private final OutFiles outFiles;

    public EasyExcelService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    /** 注解式写：一行搞定（表头来自 Order 的 @ExcelProperty）。 */
    public File write(int rows) {
        File file = outFiles.of("easyexcel-orders.xlsx");
        EasyExcel.write(file, Order.class)
                .sheet(ExcelConstants.SHEET_ORDERS)
                .doWrite(Order.samples(rows));
        return file;
    }

    /** 导出 CSV：不要样式、不要多 sheet 时，CSV 是体积与速度的最优解（也最好被下游系统解析）。 */
    public File writeCsv(int rows) {
        File file = outFiles.of("easyexcel-orders.csv");
        EasyExcel.write(file, Order.class)
                .excelType(ExcelTypeEnum.CSV)
                .sheet(ExcelConstants.SHEET_ORDERS)
                .doWrite(Order.samples(rows));
        return file;
    }

    /**
     * 监听器分批读（内存恒定）。
     *
     * @param batchSize 每批条数；攒够一批才回调一次——这正是"批量入库"的接入点。
     */
    public ReadResult readByListener(File file, int batchSize) {
        AtomicInteger total = new AtomicInteger();
        AtomicInteger batches = new AtomicInteger();
        EasyExcel.read(file, Order.class, new PageReadListener<Order>(dataList -> {
            total.addAndGet(dataList.size());
            batches.incrementAndGet();
        }, batchSize)).sheet().doRead();
        return new ReadResult(total.get(), batches.get());
    }

    /**
     * 模板填充：先生成一个带 {@code {字段}} 占位符的模板，再把占位符行复制 rows 遍、逐格替换成真实值。
     *
     * <p><b>为何不走 EasyExcel 的 withTemplate().fill()</b>：在 POI 5.5.1 + EasyExcel 4.0.3 组合下，
     * 其列表填充（即便加 {@code forceNewRow(true)}）实测不会展开，只会留一行空数据——
     * 它的模板引擎对"由 POI 程序化生成的模板"存在识别盲区。这里改用 POI 原生实现，
     * 逻辑零黑盒，也正好揭示"模板填充 = 逐行复制 + 字符串替换"的本质。
     */
    public File fillTemplate(int rows) throws IOException {
        File template = createTemplate();
        File out = outFiles.of("easyexcel-template-filled.xlsx");
        try (Workbook workbook = WorkbookFactory.create(template);
             OutputStream os = Files.newOutputStream(out.toPath())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row templateRow = sheet.getRow(1); // 占位符行

            // 收集每列的占位符（如 {orderNo}）
            Map<Integer, String> placeholderByCol = new LinkedHashMap<>();
            for (Cell cell : templateRow) {
                if (cell.getCellType() == CellType.STRING) {
                    placeholderByCol.put(cell.getColumnIndex(), cell.getStringCellValue());
                }
            }

            List<Order> data = Order.samples(rows);
            // 第 1 行即第一条数据（直接覆盖占位符行），之后逐行新建 → 末尾共 1 表头 + rows 数据
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(1 + i);
                Map<String, Object> values = toMap(data.get(i));
                for (Map.Entry<Integer, String> entry : placeholderByCol.entrySet()) {
                    String field = entry.getValue();            // {orderNo}
                    field = field.substring(1, field.length() - 1); // 去花括号 → orderNo
                    Object value = values.get(field);
                    Cell cell = row.createCell(entry.getKey());
                    if (value instanceof BigDecimal) {
                        cell.setCellValue(((BigDecimal) value).doubleValue());
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(value));
                    }
                }
            }
            workbook.write(os);
        }
        return out;
    }

    /** Order → 字段名→值 映射，供占位符替换用（保持插入顺序，列对齐更稳）。 */
    private static Map<String, Object> toMap(Order order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("orderNo", order.getOrderNo());
        map.put("customer", order.getCustomer());
        map.put("product", order.getProduct());
        map.put("quantity", order.getQuantity());
        map.put("amount", order.getAmount());
        map.put("status", order.getStatus());
        return map;
    }

    /** 统计文件真实行数（含表头）——用 POI 直接数，最朴素也最可靠，避开 EasyExcel 读 Map 的表头/列号歧义。 */
    public int readRowCount(File file) throws IOException {
        try (org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(file)) {
            return workbook.getSheetAt(0).getLastRowNum() + 1; // lastRowNum 是 0-based
        }
    }

    /** 生成模板文件：表头 + 一行 {字段名} 占位符（无需手工在 Excel 里画模板）。 */
    private File createTemplate() throws IOException {
        File template = outFiles.of("easyexcel-template.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream os = Files.newOutputStream(template.toPath())) {
            Sheet sheet = workbook.createSheet(ExcelConstants.SHEET_ORDERS);
            Row header = sheet.createRow(0);
            for (int i = 0; i < ExcelConstants.HEADERS.length; i++) {
                header.createCell(i).setCellValue(ExcelConstants.HEADERS[i]);
            }
            Row placeholder = sheet.createRow(1);
            // 列表填充占位符匹配 Java 字段名（与 @ExcelProperty 中文值无关），故用英文。
            String[] fields = {"{orderNo}", "{customer}", "{product}", "{quantity}", "{amount}", "{status}"};
            for (int i = 0; i < fields.length; i++) {
                placeholder.createCell(i).setCellValue(fields[i]);
            }
            workbook.write(os);
        }
        return template;
    }

    /** 控制台 / 测试统一入口。 */
    public String run() throws IOException {
        int rows = ExcelConstants.DEFAULT_ROWS;
        File xlsx = write(rows);
        File csv = writeCsv(rows);
        ReadResult read = readByListener(xlsx, 500);
        File filled = fillTemplate(rows);

        return String.format(
                "写 xlsx %d 行 -> %s%n写 CSV  %d 行 -> %s（同数据体积对比）%n"
                        + "监听器分批读(每批 500) -> 共 %d 行, 回调 %d 批%n"
                        + "模板填充 -> %s, 读回 %d 行（含表头）%n",
                rows, OutFiles.readableSize(xlsx),
                rows, OutFiles.readableSize(csv),
                read.total, read.batches,
                OutFiles.readableSize(filled), readRowCount(filled));
    }

    /** 分批读结果。 */
    public static class ReadResult {
        private final int total;
        private final int batches;

        public ReadResult(int total, int batches) {
            this.total = total;
            this.batches = batches;
        }

        public int getTotal() {
            return total;
        }

        public int getBatches() {
            return batches;
        }
    }
}
