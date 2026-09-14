package lan.chaos.excel.read;

import lan.chaos.excel.common.constant.ExcelConstants;
import lan.chaos.excel.common.model.Order;
import lan.chaos.excel.common.util.OutFiles;
import lan.chaos.excel.common.util.Rows;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * 能力三：大文件的两种读法——<b>用户模型（全内存）</b> vs <b>SAX 事件模型（流式）</b>。
 *
 * <p><b>WHY（痛点）</b>：一个 50MB 的 xlsx，用 {@code WorkbookFactory.create()} 读进来，
 * 堆内存能吃掉 <b>文件大小的 5~10 倍</b>（DOM 树 + 样式 + 字符串缓存），
 * 线上就是"导入功能一跑就 Full GC / OOM"。根因是 XSSF 把整个 XML 展开成对象树。
 *
 * <p><b>关键 API</b>：SAX 路线是 {@code OPCPackage} → {@code XSSFReader} →
 * {@code XMLHelper.newXMLReader()} → {@code XSSFSheetXMLHandler} + {@code SheetContentsHandler}，
 * 它<b>边读边回调</b>，内存恒定，与文件行数无关。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li>SAX 是<b>单向</b>的：只能顺序读，读到哪算哪，不能 {@code getRow(5)} 随机访问，
 *       也不能回头改——需要随机访问就只能付出全内存的代价。</li>
 *   <li>SAX 回调里拿到的是<b>格式化后的字符串</b>（{@code DataFormatter} 处理过），
 *       日期/金额要自己再解析；用户模型则直接给 {@code Date}/{@code double}。</li>
 *   <li>SAX 跳过的空单元格：Excel 会省略为空的 {@code <c/>}，
 *       所以"第 7 列没值"在 SAX 里表现为"少回调一次"，<b>不能按出现次数当列号</b>，
 *       必须解析 cellReference（如 {@code G12}）判断列位置。</li>
 *   <li>用户模型读大文件还有个隐性坑：即便你只遍历不保存，POI 仍会为每行建对象，
 *       且 <code>Row</code> 迭代不会释放——必须<b>边读边处理边丢弃</b>。</li>
 *   <li><b>SAX 的"内存恒定"是有前提的</b>：它省掉的是"单元格对象树"，
 *       但 xlsx 的<b>共享字符串表（SharedStringsTable）是整表一次性进内存的</b>。
 *       字符串占比高的文件，SAX 内存照样随文件增大——这也是本 Demo 压测里
 *       SAX 内存未必低于用户模型的原因。字符串极多的场景，要考虑拆分或专用格式。</li>
 * </ol>
 */
@Service
public class BigFileReadService {

    private final OutFiles outFiles;

    public BigFileReadService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    /** 造一个供读取对比的大文件（用 SXSSF 流式写，避免生成阶段自己先 OOM）。 */
    public File prepareBigFile(int rows) throws IOException {
        File file = outFiles.of("big-orders.xlsx");
        SXSSFWorkbook workbook = new SXSSFWorkbook(ExcelConstants.SXSSF_DEFAULT_WINDOW);
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            Sheet sheet = workbook.createSheet(ExcelConstants.SHEET_ORDERS);
            Rows.writeHeader(workbook, sheet, ExcelConstants.HEADERS);
            CellStyle moneyStyle = Rows.moneyStyle(workbook);
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
            }
            workbook.write(os);
        } finally {
            workbook.dispose();
        }
        return file;
    }

    /** 用户模型读取（全内存）：API 最舒服，内存与文件大小成正比。 */
    public int readByUserModel(File file) throws IOException {
        int count = 0;
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue; // 跳过表头
                }
                count++;
            }
        }
        return count;
    }

    /** SAX 事件模型读取（流式）：内存恒定，与行数无关。返回 [行数, 首行样本]。 */
    public SaxReadResult readBySax(File file) throws Exception {
        SaxCollector collector = new SaxCollector();
        try (OPCPackage pkg = OPCPackage.open(file, PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    XMLReader parser = XMLHelper.newXMLReader();
                    parser.setContentHandler(new XSSFSheetXMLHandler(
                            reader.getStylesTable(), reader.getSharedStringsTable(),
                            collector, new DataFormatter(), false));
                    parser.parse(new InputSource(sheetStream));
                }
            }
        }
        return new SaxReadResult(collector.rowCount, collector.sample());
    }

    /** 控制台 / 测试统一入口。 */
    public String run() throws Exception {
        int rows = ExcelConstants.DEFAULT_ROWS;
        File file = prepareBigFile(rows);
        int byUserModel = readByUserModel(file);
        SaxReadResult bySax = readBySax(file);
        return String.format("大文件 %s（%d 行）%n  用户模型读取 -> %d 行%n  SAX 事件模型 -> %d 行, 首行样本: %s%n",
                OutFiles.readableSize(file), rows, byUserModel, bySax.rowCount, bySax.firstRowSample);
    }

    /** SAX 读取结果：行数 + 首行样本（保证"可观察"）。 */
    public static class SaxReadResult {
        private final int rowCount;
        private final String firstRowSample;

        public SaxReadResult(int rowCount, String firstRowSample) {
            this.rowCount = rowCount;
            this.firstRowSample = firstRowSample;
        }

        public int getRowCount() {
            return rowCount;
        }

        public String getFirstRowSample() {
            return firstRowSample;
        }
    }

    /** 收集 SAX 回调：统计行数，并拼出第一行的前几列作为样本。 */
    private static final class SaxCollector implements XSSFSheetXMLHandler.SheetContentsHandler {

        private int rowCount;
        private int currentRow;
        private final StringBuilder firstRowSample = new StringBuilder();

        public void startRow(int rowNum) {
            currentRow = rowNum;
            if (rowNum > 0) {
                rowCount++;
            }
        }

        public void endRow(int rowNum) {
            currentRow = -1;
        }

        public void cell(String cellReference, String formattedValue,
                         org.apache.poi.xssf.usermodel.XSSFComment comment) {
            // 只取首行（rowNum==1，因为 0 是表头）的前 4 列做样本
            if (currentRow == 1 && firstRowSample.length() < 60 && cellReference != null
                    && cellReference.matches("[A-D]\\d+")) {
                if (firstRowSample.length() > 0) {
                    firstRowSample.append(" | ");
                }
                firstRowSample.append(cellReference).append('=').append(formattedValue);
            }
        }

        String sample() {
            return firstRowSample.toString();
        }
    }
}
