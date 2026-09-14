package lan.chaos.word.bigdoc;

import lan.chaos.word.common.util.OutFiles;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * 能力六：大文档内存模型（Word 与 Excel 最本质的差异）。
 *
 * <p><b>WHY（痛点）</b>：Excel 有 SXSSF 这种「滑动窗口、刷盘即丢」的流式写模型，百万行也能压内存；
 * 但 <b>Word 的 XWPF 没有等价物</b>——{@code XWPFDocument} 必须整篇常驻内存，写盘是一次性的。
 * 这意味着文档越大，内存越高，十万级以上段落很容易 OOM。这是选型时必须先想清楚的前提。
 *
 * <p><b>关键 API</b>：仍是 {@code XWPFDocument}，但本类重点不在 API，而在「用数据说话」——
 * 生成 N 段，记录耗时与内存增量。
 *
 * <p><b>生产坑</b>：
 * <ol>
 *   <li><b>Word 没有 SXSSF</b>：别指望像 Excel 那样流式导出大文档，
 *       超大文档要拆成多个子文档，或绕过 XWPF 直操作底层 document.xml。</li>
 *   <li><b>段落越多内存越高</b>：本横评会直观展示「段落数 ↔ 内存」的正相关。</li>
 * </ol>
 */
@Service
public class BigDocService {

    private final OutFiles outFiles;

    public BigDocService(OutFiles outFiles) {
        this.outFiles = outFiles;
    }

    public String run(int paragraphs) throws Exception {
        Result r = generate(paragraphs);
        return toMarkdown(paragraphs, r);
    }

    /** 生成 N 段文档，记录耗时与堆内存增量（生成前后的差值）。 */
    public Result generate(int paragraphs) throws Exception {
        File file = outFiles.of("word-big.docx");
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long memBefore = mem.getHeapMemoryUsage().getUsed();
        long t0 = System.currentTimeMillis();
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream os = new FileOutputStream(file)) {
            for (int i = 0; i < paragraphs; i++) {
                doc.createParagraph().createRun().setText(
                        "第 " + i + " 段：用于压测的示例文本，观察 XWPF 全内存模型的耗时与内存增长。");
            }
            doc.write(os);
        }
        long elapsed = System.currentTimeMillis() - t0;
        long memAfter = mem.getHeapMemoryUsage().getUsed();
        return new Result(elapsed, (memAfter - memBefore) / (1024 * 1024), file);
    }

    private String toMarkdown(int paragraphs, Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("| 方案 | 段落数 | 耗时(ms) | 内存增量(MB) | 文件大小 |%n"));
        sb.append("| --- | --- | --- | --- | --- |%n");
        sb.append(String.format("| XWPF(全内存) | %d | %d | %d | %s |%n",
                paragraphs, r.elapsed, r.memDeltaMb, OutFiles.readableSize(r.file)));
        sb.append(String.format("%n  ⚠ Word 没有 SXSSF 那样的流式写模型：XWPFDocument 必须整篇在内存里构建，%n"
                + "    段落数越多内存越高，超大规模（十万级）会 OOM；%n"
                + "    替代方案是拆成多个子文档，或绕过 XWPF 直接操作底层 document.xml。%n"));
        return sb.toString();
    }

    /** 一次生成的结果（耗时 / 内存增量 / 产物）。 */
    public static class Result {
        public final long elapsed;
        public final long memDeltaMb;
        public final File file;

        Result(long elapsed, long memDeltaMb, File file) {
            this.elapsed = elapsed;
            this.memDeltaMb = memDeltaMb;
            this.file = file;
        }
    }
}
