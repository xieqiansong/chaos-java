package lan.chaos.excel.importer;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.common.model.Order;
import lan.chaos.excel.common.util.OutFiles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 能力六：导入校验与错误行回写。
 *
 * <p><b>WHY（痛点）</b>：真实系统的导入功能，难点从来不是"读进来"，而是"读进来之后"——
 * 用户上传的表里总有几行脏数据。如果一遇脏数据就整批抛异常，用户得到的反馈只有"导入失败"，
 * 他根本不知道第几行错了、错在哪，只能一行行肉眼看。<b>正确的产品形态是：好的入库、坏的列清单</b>。
 *
 * <p><b>关键 API</b>：{@code AnalysisEventListener}（逐行回调，可拿行号）、
 * {@code context.readRowHolder().getRowIndex()}（定位错误行）、
 * 最后把错误明细单独导出成一个 xlsx 给用户下载。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>别在监听器里直接抛异常</b>：一抛就中断整个导入，前面读的行全废。
 *       正确做法是<b>收集</b>错误、继续跑、最后统一反馈。</li>
 *   <li><b>行号要换算</b>：{@code getRowIndex()} 是 0-based 且不含表头，
 *       报给用户看的行号通常是 Excel 里的行号（+2：表头 1 行 + 1-based）。</li>
 *   <li><b>数值读到的是 String 还是 BigDecimal，取决于单元格类型</b>：
 *       用户手填的数字常被 Excel 存成文本，直接 {@code (Integer) value} 会 ClassCastException。
 *       本 Demo 用 typed model 由 EasyExcel 做转换，但它转换失败时会把该字段置 null——
 *       所以校验里必须同时判 <code>null</code> 与<b>业务非法</b>两种情况。</li>
 *   <li><b>校验要一次报全</b>：一行里有 3 个错就报 3 条，别报第一条就 return，
 *       否则用户要来回导 3 次。</li>
 * </ol>
 */
@Service
public class ImportCheckService {

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final int MAX_QUANTITY = 1000;

    private final OutFiles outFiles;

    public ImportCheckService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    /** 造一份"用户上传"的脏数据表：正常行 + 4 行典型脏数据。 */
    public File prepareDirtyFile(int validRows) {
        File file = outFiles.of("import-dirty.xlsx");
        List<Order> data = new ArrayList<>(Order.samples(validRows));
        data.add(new Order("", "客户-脏", "商品-A", 5, new BigDecimal("10.00"), STATUS_PAID, new Date()));
        data.add(new Order("SO-9001", "客户-脏", "商品-B", 0, new BigDecimal("10.00"), STATUS_PAID, new Date()));
        data.add(new Order("SO-9002", "客户-脏", "商品-C", 3, new BigDecimal("-1.00"), STATUS_PAID, new Date()));
        data.add(new Order("SO-9003", "客户-脏", "商品-D", 1, new BigDecimal("10.00"), "UNKNOWN", new Date()));
        EasyExcel.write(file, Order.class).sheet(ExcelConstants.SHEET_ORDERS).doWrite(data);
        return file;
    }

    /** 逐行校验：好的计数、坏的收集，绝不因单行脏数据中断。 */
    public ImportResult check(File file) {
        List<ErrorRow> errors = new ArrayList<>();
        int[] counters = new int[2]; // [0]=总数 [1]=有效数

        EasyExcel.read(file, Order.class, new AnalysisEventListener<Order>() {
            public void invoke(Order order, AnalysisContext context) {
                counters[0]++;
                String reason = validate(order);
                if (reason == null) {
                    counters[1]++;
                    return;
                }
                // rowIndex 是 0-based 的数据行号；+2 换算成用户在 Excel 里看到的行号（表头 + 1-based）
                int excelRow = context.readRowHolder().getRowIndex() + 2;
                errors.add(new ErrorRow(excelRow, order.getOrderNo(), reason));
            }

            public void doAfterAllAnalysed(AnalysisContext context) {
                // 真实项目里：这里做"有效数据批量入库"
            }
        }).sheet().doRead();

        return new ImportResult(counters[0], counters[1], errors);
    }

    /** 把错误明细导出成 xlsx，让用户照着改完重传。 */
    public File writeErrorReport(ImportResult result) {
        File file = outFiles.of("import-errors.xlsx");
        EasyExcel.write(file, ErrorRow.class).sheet("错误明细").doWrite(result.getErrors());
        return file;
    }

    /** 单个 Order 的业务校验：一次把所有问题报全（用「；」连接），无问题返回 null。 */
    public String validate(Order order) {
        List<String> reasons = new ArrayList<>();
        if (order.getOrderNo() == null || !order.getOrderNo().startsWith("SO-")) {
            reasons.add("订单号为空或格式错误（应以 SO- 开头）");
        }
        if (order.getQuantity() == null || order.getQuantity() <= 0 || order.getQuantity() > MAX_QUANTITY) {
            reasons.add("数量非法（应 1~" + MAX_QUANTITY + "）");
        }
        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            reasons.add("金额非法（应大于 0）");
        }
        if (order.getStatus() == null
                || (!STATUS_PAID.equals(order.getStatus()) && !STATUS_CANCELLED.equals(order.getStatus()))) {
            reasons.add("状态非法（应为 PAID / CANCELLED）");
        }
        return reasons.isEmpty() ? null : String.join("；", reasons);
    }

    /** 控制台 / 测试统一入口。 */
    public String run() {
        File dirty = prepareDirtyFile(ExcelConstants.DEFAULT_ROWS);
        ImportResult result = check(dirty);
        File report = result.getErrors().isEmpty() ? null : writeErrorReport(result);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("导入 %d 行 -> 有效 %d 行, 错误 %d 行%n",
                result.getTotal(), result.getValid(), result.getErrors().size()));
        for (ErrorRow error : result.getErrors()) {
            sb.append(String.format("   第 %d 行 [%s] %s%n", error.getRowIndex(), error.getOrderNo(), error.getReason()));
        }
        if (report != null) {
            sb.append(String.format("错误报告 -> %s (%s)%n", report.getName(), OutFiles.readableSize(report)));
        }
        return sb.toString();
    }

    /** 导入结果：总数 / 有效数 / 错误明细。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportResult {
        private int total;
        private int valid;
        private List<ErrorRow> errors;
    }

    /** 一行错误明细（带 @ExcelProperty，可直接被 EasyExcel 导出）。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorRow {
        @com.alibaba.excel.annotation.ExcelProperty("行号")
        private int rowIndex;
        @com.alibaba.excel.annotation.ExcelProperty("订单号")
        private String orderNo;
        @com.alibaba.excel.annotation.ExcelProperty("错误原因")
        private String reason;
    }
}
