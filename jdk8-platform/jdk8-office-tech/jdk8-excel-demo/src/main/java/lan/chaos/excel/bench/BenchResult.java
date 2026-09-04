package lan.chaos.excel.bench;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 压测结果的 markdown 表格。
 *
 * <p>WHY：横评的结论必须<b>落盘可读</b>（target/bench-results.md），
 * 而不是只在控制台一闪而过——机器的耗时数字会随环境波动，留存下来才能跨版本对比。
 * 与仓库内其他压测型 Demo（ratelimiter / batch-ingest / servlet-filter-async）保持同一套路。
 */
public class BenchResult {

    private final String title;
    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();

    public BenchResult(String title, String... headers) {
        this.title = title;
        this.headers = Arrays.asList(headers);
    }

    /** 加一行数据（自动 String 化，失败行也照样记录，不中断整轮压测）。 */
    public BenchResult addRow(Object... cells) {
        List<String> row = new ArrayList<>();
        for (Object cell : cells) {
            row.add(String.valueOf(cell));
        }
        rows.add(row);
        return this;
    }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(title).append("\n\n");
        sb.append('|');
        for (String header : headers) {
            sb.append(' ').append(header).append(" |");
        }
        sb.append("\n|");
        for (int i = 0; i < headers.size(); i++) {
            sb.append(" --- |");
        }
        sb.append('\n');
        for (List<String> row : rows) {
            sb.append('|');
            for (String cell : row) {
                sb.append(' ').append(cell).append(" |");
            }
            sb.append('\n');
        }
        return sb.append('\n').toString();
    }

    public File writeTo(File file) throws IOException {
        Files.write(file.toPath(), toMarkdown().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /** 追加一段（多组横评写进同一个文件）。 */
    public File appendTo(File file) throws IOException {
        Files.write(file.toPath(), toMarkdown().getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        return file;
    }

    /** 已用内存（粗略）：totalMemory - freeMemory。测量前建议先 System.gc()。 */
    public static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /** 字节数 → MB 字符串。 */
    public static String toMb(long bytes) {
        return String.format("%.1f", bytes / (1024.0 * 1024.0));
    }
}
