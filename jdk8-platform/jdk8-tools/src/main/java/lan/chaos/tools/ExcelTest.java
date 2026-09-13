package lan.chaos.tools;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.metadata.Head;
import cn.idev.excel.write.style.column.AbstractHeadColumnWidthStyleStrategy;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import cn.idev.excel.write.style.column.SimpleColumnWidthStyleStrategy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
    <dependency>
        <groupId>cn.idev.excel</groupId>
        <artifactId>fastexcel</artifactId>
        <version>1.2.0</version>
    </dependency>
 */
public class ExcelTest {
    private static void excelTest() throws IOException {
        OutputStream outputStream = Files.newOutputStream(Paths.get("D:/tmp/test.xlsx"));
        List<List<String>> headers = new ArrayList<>();
        headers.add(Arrays.asList("名称", "名称", "名称"));
        headers.add(Arrays.asList("合计", "合计", "类别一"));
        headers.add(Arrays.asList("合计", "合计", "类别二"));
        headers.add(Arrays.asList("合计", "合计", "类别三"));

        // 定义自定义处理器来精确设置第一列宽度
        AbstractHeadColumnWidthStyleStrategy firstColumnWidthHandler = new AbstractHeadColumnWidthStyleStrategy() {
            @Override
            protected Integer columnWidth(Head head, Integer columnIndex) {
                if (columnIndex == 0) {
                    return 35;
                }
                return null;
            }
        };

        EasyExcel.write(outputStream).head(headers)
                .automaticMergeHead(true)
                // 注册自适应列宽策略
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                // 注册自适应列宽策略
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(4))
                // 注册自定义处理器
                .registerWriteHandler(firstColumnWidthHandler)
                .sheet().doWrite(new ArrayList<>());
    }
}
