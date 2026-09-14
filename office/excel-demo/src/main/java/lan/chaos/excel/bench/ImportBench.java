package lan.chaos.excel.bench;

import lan.chaos.excel.easyexcel.EasyExcelService;
import lan.chaos.excel.hutool.HutoolExcelService;
import lan.chaos.excel.read.BigFileReadService;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 横评二：<b>导入</b>——同一个 xlsx，三种读法的耗时 / 内存对比。
 *
 * <p>参与方：用户模型（全内存）、SAX 事件模型（流式）、EasyExcel 监听器（流式）、Hutool 读（全内存封装）。
 *
 * <p><b>怎么读这张表</b>：
 * <ul>
 *   <li>用户模型 API 最舒服，但内存与文件大小成正比，是"导入 OOM"的根因。</li>
 *   <li>SAX 内存恒定，代价是只能顺序读、且要自己处理空单元格与类型转换。</li>
 *   <li>EasyExcel 监听器在 SAX 之上补了类型转换与分批回调，
 *       <b>日常业务导入的首选</b>：既省内存，又能攒批批量入库。</li>
 * </ul>
 */
@Component
public class ImportBench {

    private final BigFileReadService readService;
    private final EasyExcelService easyExcelService;
    private final HutoolExcelService hutoolExcelService;

    public ImportBench(BigFileReadService readService, EasyExcelService easyExcelService,
                       HutoolExcelService hutoolExcelService) {
        this.readService = readService;
        this.easyExcelService = easyExcelService;
        this.hutoolExcelService = hutoolExcelService;
    }

    public BenchResult run(int rows) throws Exception {
        File file = readService.prepareBigFile(rows);
        BenchResult result = new BenchResult("导入横评：读 " + rows + " 行",
                "方案", "耗时(ms)", "内存增量(MB)", "读到行数");
        bench(result, "用户模型（全内存）", () -> readService.readByUserModel(file));
        bench(result, "SAX 事件模型", () -> readService.readBySax(file).getRowCount());
        bench(result, "EasyExcel 监听器（每批 1000）", () -> easyExcelService.readByListener(file, 1000).getTotal());
        bench(result, "Hutool 读（全内存封装）", () -> hutoolExcelService.readRows(file));
        return result;
    }

    private void bench(BenchResult result, String name, ReadTask task) {
        try {
            System.gc();
            Thread.sleep(50);
            long memoryBefore = BenchResult.usedMemory();
            long start = System.nanoTime();
            int rowCount = task.read();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            long memoryDelta = BenchResult.usedMemory() - memoryBefore;
            result.addRow(name, elapsedMs, BenchResult.toMb(memoryDelta), rowCount);
        } catch (Exception e) {
            result.addRow(name, "失败", e.getClass().getSimpleName(), "-");
        }
    }

    @FunctionalInterface
    private interface ReadTask {
        int read() throws Exception;
    }
}
