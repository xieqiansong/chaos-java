package lan.chaos.excel;

import lan.chaos.excel.basic.BasicExcelService;
import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.easyexcel.EasyExcelService;
import lan.chaos.excel.hutool.HutoolExcelService;
import lan.chaos.excel.importer.ImportCheckService;
import lan.chaos.excel.read.BigFileReadService;
import lan.chaos.excel.write.StyleWriteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 每个能力一条可断言的测试：纯本地文件、零外部依赖，CI 直接跑。
 *
 * <p>验证重点是"能用"而不是"好看"：写出去的文件必须能被读回来，且行数/数值一致——
 * 这是导入导出类代码唯一的强验证。
 */
@SpringBootTest
class ExcelScenarioTest {

    private static final int ROWS = ExcelConstants.DEFAULT_ROWS;

    @Autowired
    private BasicExcelService basic;
    @Autowired
    private StyleWriteService style;
    @Autowired
    private BigFileReadService bigFileRead;
    @Autowired
    private EasyExcelService easyExcel;
    @Autowired
    private ImportCheckService importer;
    @Autowired
    private HutoolExcelService hutool;

    @Test
    void basic_threeWorkbookTypesWriteAndReadBack() throws IOException {
        assertEquals(ROWS, basic.readRowCount(basic.writeXls(ROWS)), "HSSF(.xls) 往返行数应一致");
        assertEquals(ROWS, basic.readRowCount(basic.writeXlsx(ROWS)), "XSSF(.xlsx) 往返行数应一致");
        assertEquals(ROWS, basic.readRowCount(basic.writeSxssf(ROWS, 100)), "SXSSF 往返行数应一致");
    }

    @Test
    void basic_hssfRejectsMoreThan65536Rows() {
        assertThrows(IllegalArgumentException.class,
                () -> basic.writeXls(ExcelConstants.HSSF_MAX_ROWS + 1),
                ".xls 格式上限 65536 行，超了必须明确失败而不是写出打不开的文件");
    }

    @Test
    void basic_sxssfFlushedRowCannotBeReadBack() throws IOException {
        assertTrue(basic.demoFlushedRowsAreGone().contains("null"),
                "SXSSF 刷出窗口的行应取不回来——这是'导出中途改样式'失败的根因");
    }

    @Test
    void write_formulaMustBeEvaluatedBeforeReading() throws IOException {
        File report = style.writeReport(200);
        double total = style.readTotal(report);
        assertTrue(total > 0, "公式必须先求值再读，否则读回来是 0（实际 " + total + "）");
    }

    @Test
    void read_userModelAndSaxMustAgreeOnRowCount() throws Exception {
        File file = bigFileRead.prepareBigFile(500);
        int byUserModel = bigFileRead.readByUserModel(file);
        int bySax = bigFileRead.readBySax(file).getRowCount();
        assertEquals(byUserModel, bySax, "两种读法必须读到相同的行数");
        assertEquals(500, bySax);
    }

    @Test
    void easyexcel_writeThenReadByBatches() {
        File file = easyExcel.write(300);
        EasyExcelService.ReadResult result = easyExcel.readByListener(file, 100);
        assertEquals(300, result.getTotal(), "应读回全部 300 行");
        assertEquals(3, result.getBatches(), "每批 100 条 → 300 行应触发 3 次回调（攒批入库的接入点）");
    }

    @Test
    void easyexcel_templateFillProducesHeaderPlusDataRows() throws IOException {
        File filled = easyExcel.fillTemplate(50);
        assertEquals(51, easyExcel.readRowCount(filled), "模板填充后应为 1 行表头 + 50 行数据");
    }

    @Test
    void easyexcel_csvAndXlsxBothProduced() {
        File xlsx = easyExcel.write(ROWS);
        File csv = easyExcel.writeCsv(ROWS);
        assertTrue(csv.length() > 0 && xlsx.length() > 0);
        // 同数据量下 xlsx 是 zip 压缩的 OOXML，通常比 CSV 小；CSV 的价值在"通用可解析"而非体积，
        // 这里只断言两者都成功产出（体积对比交给 bench 实测，避免环境差异导致断言脆弱）
    }

    @Test
    void importCheck_dirtyRowsAreCollectedInsteadOfAborting() {
        File dirty = importer.prepareDirtyFile(100); // 100 正常行 + 4 行脏数据
        ImportCheckService.ImportResult result = importer.check(dirty);

        assertEquals(104, result.getTotal(), "脏数据不能中断导入，总数必须是全部行");
        assertEquals(100, result.getValid(), "正常行应全部通过校验");
        assertEquals(4, result.getErrors().size(), "4 行脏数据应被逐条收集");
        assertTrue(result.getErrors().get(0).getReason().contains("订单号"), "错误原因要具体到字段");
    }

    @Test
    void importCheck_errorReportCanBeExported() {
        File dirty = importer.prepareDirtyFile(20);
        ImportCheckService.ImportResult result = importer.check(dirty);
        File report = importer.writeErrorReport(result);
        assertTrue(report.length() > 0, "错误明细要能导出成 xlsx 给用户下载");
    }

    @Test
    void hutool_writeAndReadBack() {
        File file = hutool.write(100);
        assertEquals(100, hutool.readRows(file), "Hutool 写出 100 行应原样读回");
    }
}
