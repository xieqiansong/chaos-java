package lan.chaos.excel.bench;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lan.chaos.excel.common.model.Order;
import lan.chaos.excel.common.util.OutFiles;
import lan.chaos.excel.common.util.Rows;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * 横评一：<b>导出</b>——同样 7 列数据，五种写法的耗时 / 内存 / 体积对比。
 *
 * <p>参与方：XSSF（全内存）、SXSSF（窗口 100 / 1000）、EasyExcel、CSV。
 *
 * <p><b>怎么读这张表</b>：
 * <ul>
 *   <li>XSSF 是最直观的写法，但内存随行数线性涨，是"导出 OOM"的头号元凶。</li>
 *   <li>SXSSF 用滑动窗口把内存压成常量，代价是<b>刷出的行不能再改</b>。</li>
 *   <li>窗口越大越快、越吃内存；默认 100 偏保守，导出大文件常设 500~2000。</li>
 *   <li>EasyExcel 内部就是 SXSSF，只是把细节封装好了，性能与 SXSSF 同量级。</li>
 *   <li>CSV 没有样式、没有多 sheet，但体积最小、最快，是"纯数据交接"场景的最优解。</li>
 * </ul>
 */
@Component
public class ExportBench {

    private final OutFiles outFiles;

    public ExportBench(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public BenchResult run(int rows) {
        BenchResult result = new BenchResult("导出横评：" + rows + " 行 × 7 列",
                "方案", "耗时(ms)", "内存增量(MB)", "文件大小");
        bench(result, "XSSF（全内存）", rows, this::xssf);
        bench(result, "SXSSF（窗口=100）", rows, r -> sxssf(r, 100));
        bench(result, "SXSSF（窗口=1000）", rows, r -> sxssf(r, 1000));
        bench(result, "EasyExcel", rows, this::easyExcel);
        bench(result, "CSV（EasyExcel）", rows, this::csv);
        return result;
    }

    private File xssf(int rows) throws IOException {
        File file = outFiles.of("bench-xssf.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream os = Files.newOutputStream(file.toPath())) {
            Rows.fillOrders(workbook, rows);
            workbook.write(os);
        }
        return file;
    }

    private File sxssf(int rows, int windowSize) throws IOException {
        File file = outFiles.of("bench-sxssf-" + windowSize + ".xlsx");
        SXSSFWorkbook workbook = new SXSSFWorkbook(windowSize);
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            Rows.fillOrders(workbook, rows);
            workbook.write(os);
        } finally {
            workbook.dispose();
        }
        return file;
    }

    private File easyExcel(int rows) {
        File file = outFiles.of("bench-easyexcel.xlsx");
        EasyExcel.write(file, Order.class).sheet("订单").doWrite(Order.samples(rows));
        return file;
    }

    private File csv(int rows) {
        File file = outFiles.of("bench-easyexcel.csv");
        EasyExcel.write(file, Order.class).excelType(ExcelTypeEnum.CSV).sheet("订单").doWrite(Order.samples(rows));
        return file;
    }

    /** 计时 + 采内存 + 记体积；单个方案失败不影响其他方案。 */
    private void bench(BenchResult result, String name, int rows, ExportTask task) {
        try {
            System.gc();
            Thread.sleep(50);
            long memoryBefore = BenchResult.usedMemory();
            long start = System.nanoTime();
            File file = task.write(rows);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            long memoryDelta = BenchResult.usedMemory() - memoryBefore;
            result.addRow(name, elapsedMs, BenchResult.toMb(memoryDelta), OutFiles.readableSize(file));
        } catch (Exception e) {
            result.addRow(name, "失败", e.getClass().getSimpleName(), "-");
        }
    }

    @FunctionalInterface
    private interface ExportTask {
        File write(int rows) throws IOException;
    }
}
