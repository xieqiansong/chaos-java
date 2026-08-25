package lan.chaos.batchwriter.bench;

import lan.chaos.batchwriter.config.BatchWriterProperties;
import lan.chaos.batchwriter.writer.AdaptiveBatchWriter;
import lan.chaos.batchwriter.writer.BatchWriter;
import lan.chaos.batchwriter.writer.BatchWriterFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SpringBootTest 基准：一条 {@code mvn test} 跑完整场景矩阵，自动写 markdown 报告。
 *
 * <p>激活 {@code local} profile，经本项目的 application-local.yml（gitignore）读入真实 Redis 连接。
 * 各场景使用独立 key，避免互相污染。
 */
@SpringBootTest
@ActiveProfiles("local")
class BenchMarkTest {

    private static final int DURATION_SEC = 12;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private BatchWriterProperties props;

    /** 一个压测场景。 */
    private static final class Scenario {
        final String name;
        final String mode;
        final int rate;
        final boolean flood;

        Scenario(String name, String mode, int rate, boolean flood) {
            this.name = name;
            this.mode = mode;
            this.rate = rate;
            this.flood = flood;
        }
    }

    private static List<Scenario> scenarios() {
        return Arrays.asList(
                new Scenario("01-legacy-5000", "legacy", 5000, false),
                new Scenario("02-static-5000", "static", 5000, false),
                new Scenario("03-adaptive-5000", "adaptive", 5000, false),
                new Scenario("04-legacy-flood", "legacy", 0, true),
                new Scenario("05-static-flood", "static", 0, true),
                new Scenario("06-adaptive-flood", "adaptive", 0, true)
        );
    }

    private BenchResult runOne(Scenario sc) {
        String key = "bench:" + sc.name;
        BatchWriter<String> w = BatchWriterFactory.create(sc.mode, redis, key, props);
        BenchOptions o = BenchOptions.of(8, sc.rate, DURATION_SEC, sc.flood);
        try {
            return BenchEngine.run(w, o);
        } finally {
            w.close();
        }
    }

    @Test
    void benchAllScenarios() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (Scenario sc : scenarios()) {
            BenchResult r = runOne(sc);
            rows.add(new String[]{sc.name, sc.mode,
                    String.valueOf(r.totalWritten), String.valueOf(r.itemsPerSec),
                    String.valueOf(r.redisCalls), String.valueOf(r.redisPerSec),
                    String.valueOf(r.avgBatchSize), String.valueOf(r.dropped), String.valueOf(r.errors)});
            printRow(sc.name, r);
        }
        writeMarkdown(file("bench-results.md"), rows, String[]::new,
                new String[]{"name", "mode", "totalWritten", "items/s", "redisCalls", "redis/s", "avgBatch", "dropped", "errors"});

        double legacyFlood = rowAvg(rows, "04-legacy-flood", 3);
        double adaptiveFlood = rowAvg(rows, "06-adaptive-flood", 3);
        assertTrue(adaptiveFlood > legacyFlood,
                "adaptive flood items/s(" + adaptiveFlood + ") 应显著高于 legacy(" + legacyFlood + ")");
        double adaptiveBatch = rowAvg(rows, "06-adaptive-flood", 6);
        assertTrue(adaptiveBatch > 1, "adaptive 平均批量应 > 1，实际=" + adaptiveBatch);
    }

    /**
     * 自适应收敛专项：flood 持续 180s（满足"至少 3 分钟"），
     * 采样线程每 2s 记录一次 batchSize 与队列水位的实时变化，
     * 用于观察"动态引擎"批量大小从初始值爬升 → 收敛稳定的完整轨迹。
     */
    @Test
    void adaptiveConvergence() throws Exception {
        String key = "bench:07-adaptive-convergence";
        AdaptiveBatchWriter<String> w = (AdaptiveBatchWriter<String>)
                BatchWriterFactory.create("adaptive", redis, key, props);

        List<String[]> trace = new ArrayList<>();
        AtomicBoolean stop = new AtomicBoolean(false);
        Thread sampler = new Thread(() -> {
            long t0 = System.currentTimeMillis();
            int last = -1;
            while (!stop.get()) {
                int bs = w.currentBatchSize();
                if (bs != last) {
                    long s = (System.currentTimeMillis() - t0) / 1000L;
                    trace.add(new String[]{String.valueOf(s), String.valueOf(bs), String.valueOf(w.queueSize())});
                    last = bs;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "convergence-sampler");
        sampler.setDaemon(true);
        sampler.start();

        BenchResult r;
        try {
            r = BenchEngine.run(w, BenchOptions.of(8, 0, 180, true)); // flood, 180s
        } finally {
            stop.set(true);
            w.close();
        }
        printRow("07-adaptive-conv", r);

        appendSection(file("bench-results.md"), "自适应收敛专项（flood 180s）实时轨迹",
                trace, new String[]{"t(s)", "batchSize", "queue"});
    }

    private static void printRow(String name, BenchResult r) {
        System.out.printf(Locale.ROOT, "[%-18s] items/s=%.1f redis/s=%.1f avgBatch=%.1f dropped=%d errors=%d%n",
                name, r.itemsPerSec, r.redisPerSec, r.avgBatchSize, r.dropped, r.errors);
    }

    private static double rowAvg(List<String[]> rows, String name, int col) {
        for (String[] r : rows) {
            if (r[0].equals(name)) {
                return Double.parseDouble(r[col]);
            }
        }
        return Double.MAX_VALUE;
    }

    public static void main(String[] a) {
        // 用 mvn test -Dtest=BenchMarkTest 跑即可
    }

    static String file(String name) {
        return "d:/project/chaos/chaos-java/jdk8-platform/jdk8-batch-ingest-demo/target/" + name;
    }

    static void writeMarkdown(String path, List<String[]> rows,
                              java.util.function.IntFunction<String[]> empty,
                              String[] header) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# bench-results (SpringBootTest)\n\n");
        sb.append("> 由 mvn test 生成，Redis 8 @30102，8 汇聚线程，每场景 12s。\n\n");
        renderTable(sb, rows, header);
        Files.write(Paths.get(path), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** 追加一节 markdown（用于收敛专项等非汇总结果的分段输出）。 */
    static void appendSection(String path, String sectionTitle, List<String[]> rows, String[] header) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## ").append(sectionTitle).append("\n\n");
        renderTable(sb, rows, header);
        Files.write(Paths.get(path), sb.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND);
    }

    private static void renderTable(StringBuilder sb, List<String[]> rows, String[] header) {
        for (String h : header) {
            sb.append("| ").append(h);
        }
        sb.append(" |\n");
        for (String h : header) {
            sb.append("|---");
        }
        sb.append("|\n");
        for (String[] r : rows) {
            sb.append("| ");
            sb.append(String.join(" | ", r));
            sb.append(" |\n");
        }
    }
}