package lan.chaos.excel.hutool;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.common.model.Order;
import lan.chaos.excel.common.util.OutFiles;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * 能力五：Hutool 的 Excel 轻量封装（对照）。
 *
 * <p><b>WHY（痛点）</b>：不是每个场景都需要"高性能流式导出"——
 * 内部小工具、一次性脚本、几十行的数据倒腾，用 POI 写十几个 createRow/createCell 太啰嗦。
 * Hutool 把这类场景压成三行：{@code ExcelUtil.getWriter()} → {@code write(list)} → {@code close()}。
 *
 * <p><b>关键 API</b>：{@code ExcelUtil.getWriter/getReader}、{@code addHeaderAlias(字段名, 中文表头)}、
 * {@code writer.write(list, true)}（true = 用别名当表头）。
 *
 * <p><b>生产坑（选型时必须知道）</b>：
 * <ol>
 *   <li><b>Hutool 不自带 POI</b>：hutool-all 把 poi-ooxml 声明为可选依赖，不传进来。
 *       所以 classpath 上必须有 POI，否则运行期 {@code NoClassDefFoundError}——
 *       而编译期完全正常，这个坑只在运行时炸。</li>
 *   <li><b>底层仍是 POI 的全内存模型</b>：Hutool 只是简化了 API，
 *       10 万行一样会吃满堆。大文件请用 SXSSF 或 EasyExcel。</li>
 *   <li><b>别名体系与 EasyExcel 不互通</b>：Hutool 用 {@code addHeaderAlias} / {@code @Alias}，
 *       EasyExcel 用 {@code @ExcelProperty}。同一个 model 被两套注解同时标注，
 *       是项目里最常见的一坨混乱——建议一个 model 只服务一套体系，
 *       本 Demo 里 Order 归 EasyExcel，Hutool 侧用代码显式设别名。</li>
 * </ol>
 */
@Service
public class HutoolExcelService {

    private final OutFiles outFiles;

    public HutoolExcelService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    /**
     * 三行写出：别名即表头。
     *
     * <p><b>坑（本类第 4 条，且非常隐蔽）</b>：{@code ExcelUtil.getWriter(File)} 在
     * <b>文件已存在时会直接打开它</b>，而不是新建一个空工作簿。
     * 于是「先写 1000 行、再写 100 行」之后，第 101~1000 行的<b>旧数据仍然残留</b>，
     * 回读得到的行数是 1000 而不是 100——而整个过程不报任何错。
     * 在「多次导出复用同一文件名」的后台任务里，这会直接导出脏数据。
     *
     * <p>正确做法：写入前先删除旧文件（或写到临时文件再原子替换），保证每次都是干净的工作簿。
     */
    public File write(int rows) {
        File file = outFiles.of("hutool-orders.xlsx");
        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("无法删除旧的产物文件，可能导致旧数据残留：" + file.getAbsolutePath());
        }
        try (ExcelWriter writer = ExcelUtil.getWriter(file)) {
            writer.addHeaderAlias("orderNo", "订单号");
            writer.addHeaderAlias("customer", "客户");
            writer.addHeaderAlias("product", "商品");
            writer.addHeaderAlias("quantity", "数量");
            writer.addHeaderAlias("amount", "金额");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("createdAt", "下单时间");
            writer.write(Order.samples(rows), true);
            writer.autoSizeColumnAll();
        }
        return file;
    }

    /**
     * 回读：按原始行读（List&lt;List&gt;），刻意不依赖别名反查，避免把 demo 耦死在 Hutool 的映射规则上。
     *
     * <p>注意行号区间：{@code read(startRowIndex, endRowIndex)} 的<b>两端都包含</b>，
     * 表头占 index 0，所以数据区是 1 .. getRowCount()-1。少减这个 1 是最常见的 off-by-one。
     */
    public int readRows(File file) {
        try (ExcelReader reader = ExcelUtil.getReader(file)) {
            List<List<Object>> rows = reader.read(1, reader.getRowCount() - 1);
            return rows.size();
        }
    }

    /** 控制台 / 测试统一入口。 */
    public String run() {
        int rows = ExcelConstants.DEFAULT_ROWS;
        File file = write(rows);
        return String.format("Hutool 写 %d 行 -> %s, 回读 %d 行%n",
                rows, OutFiles.readableSize(file), readRows(file));
    }
}
