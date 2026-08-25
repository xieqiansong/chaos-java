package lan.chaos.ratelimiter;

import lan.chaos.ratelimiter.RateLimiter;
import lan.chaos.ratelimiter.bench.BenchEngine;
import lan.chaos.ratelimiter.bench.BenchOptions;
import lan.chaos.ratelimiter.bench.BenchResult;
import lan.chaos.ratelimiter.local.LocalOnlyRateLimiter;
import lan.chaos.ratelimiter.local.LocalRedisRateLimiter;
import lan.chaos.ratelimiter.redis.RedisLuaRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SpringBootTest 基准：一条 `mvn test` 跑完整场景矩阵，自动写 markdown 报告。
 *
 * <ul>
 *   <li>{@link #benchAllScenarios()}：三实现通用对照（A/B/burst/skew/flood）→ <b>bench-results.md</b></li>
 *   <li>{@link #windowMsSensitivity()}：window-ms 专项（精度/成本随窗口变化）→ <b>bench-results-window-ms.md</b></li>
 * </ul>
 *
 * <p>激活 {@code local} profile，经本项目的 application-local.yml（已被 gitignore）读入真实 Redis 连接，
 * 无需跨项目取密码、无需手动设环境变量；敏感信息不落任何结果文件。
 */
@SpringBootTest
@ActiveProfiles("local")
class BenchMarkTest {

    /** 每个场景压测时长（秒）。调大更稳、更久。 */
    private static final int DURATION_SEC = 5;

    @Autowired
    private StringRedisTemplate redis;

    /** 一个压测场景：不受 local-redis 控制的参数用默认即可。 */
    private static final class Scenario {
        final String name;
        final String mode;
        final int qps;               // flood=true 时忽略
        final boolean flood;
        final double burst;          // local-redis
        final long window;           // local-redis
        final double skew;           // -1=轮询

        Scenario(String name, String mode, int qps, boolean flood,
                 double burst, long window, double skew) {
            this.name = name;
            this.mode = mode;
            this.qps = qps;
            this.flood = flood;
            this.burst = burst;
            this.window = window;
            this.skew = skew;
        }

        static Scenario local(String name, int qps, boolean flood,
                              double burst, long window, double skew) {
            return new Scenario(name, "local-redis", qps, flood, burst, window, skew);
        }

        static Scenario lua(String name, int qps, boolean flood) {
            return new Scenario(name, "redis-lua", qps, flood, 1.5, 1000, -1);
        }
    }

    /** 通用对照矩阵（不含 window 档，window 单独由 {@link #windowMsSensitivity()} 覆盖）。 */
    private static List<Scenario> scenarios() {
        return Arrays.asList(
                Scenario.lua("01-redis-lua-A-500", 500, false),
                Scenario.local("02-local-redis-A-500", 500, false, 1.5, 1000, -1),
                Scenario.lua("03-redis-lua-B-2000", 2000, false),
                Scenario.local("04-local-redis-B-2000", 2000, false, 1.5, 1000, -1),
                Scenario.local("05-local-redis-burst1", 2000, false, 1.0, 1000, -1),
                Scenario.local("06-local-redis-burst3", 2000, false, 3.0, 1000, -1),
                Scenario.local("07-local-redis-skew", 2000, false, 1.5, 1000, 0.8),
                Scenario.lua("12-redis-lua-flood", 0, true),
                Scenario.local("13-local-redis-flood", 0, true, 1.5, 1000, -1)
        );
    }

    private RateLimiter[] buildLimiters(Scenario sc, List<LocalRedisRateLimiter> closables) {
        RateLimiter[] ls = new RateLimiter[4];
        for (int i = 0; i < 4; i++) {
            switch (sc.mode) {
                case "redis-lua":
                    ls[i] = new RedisLuaRateLimiter(redis, 1000, 1000);
                    break;
                case "local-only":
                    ls[i] = new LocalOnlyRateLimiter(1000, 1000);
                    break;
                default:
                    LocalRedisRateLimiter l = new LocalRedisRateLimiter(redis, "n" + i,
                            1000, sc.window, 4, sc.burst);
                    closables.add(l);
                    ls[i] = l;
                    break;
            }
        }
        return ls;
    }

    private static void flushRedis(StringRedisTemplate t) {
        RedisConnection c = t.getConnectionFactory().getConnection();
        try {
            c.flushDb();
        } finally {
            c.close();
        }
    }

    /** 跑单个场景（隔离 key + 压测），返回结构化结果。 */
    private BenchResult runOne(Scenario sc, List<LocalRedisRateLimiter> closables) {
        flushRedis(redis);
        RateLimiter[] ls = buildLimiters(sc, closables);
        BenchOptions o = BenchOptions.of(8, sc.qps, 1000, DURATION_SEC,
                4, sc.burst, sc.window, sc.flood, sc.skew);
        return BenchEngine.run(ls, o);
    }

    private static void printRow(String name, BenchResult r) {
        System.out.printf(Locale.ROOT, "[%-22s] qps=%.1f avg=%.1fus p99=%.1fus redis/s=%.2f overLimit=%.2f%%%n",
                name, r.qps, r.avgUs, r.p99Us, r.redisPerSec, r.overLimitPct);
    }

    // ================= 通用对照 =================

    @Test
    void benchAllScenarios() throws Exception {
        List<String[]> rows = new ArrayList<>();
        List<LocalRedisRateLimiter> closables = new ArrayList<>();
        try {
            for (Scenario sc : scenarios()) {
                BenchResult r = runOne(sc, closables);
                rows.add(new String[]{sc.name, sc.mode,
                        String.valueOf(r.qps), String.valueOf(r.avgUs), String.valueOf(r.p99Us),
                        String.valueOf(r.redisPerSec), String.valueOf(r.overLimitPct)});
                printRow(sc.name, r);
            }
        } finally {
            closeAll(closables);
        }
        writeMarkdown(file("bench-results.md"), rows, String[]::new,
                new String[]{"name", "mode", "qps", "avg(us)", "p99(us)", "redis/s", "overLimit%"});

        double luaFlood = rowAvg(rows, "12-redis-lua-flood", 3);
        double localFlood = rowAvg(rows, "13-local-redis-flood", 3);
        assertTrue(localFlood > 0 && localFlood < luaFlood,
                "local-redis flood avg(" + localFlood + "us) 应低于 redis-lua flood avg(" + luaFlood + "us)");
    }

    // ================= window-ms 精度专项 =================

    @Test
    void windowMsSensitivity() throws Exception {
        int[] windows = {250, 500, 1000, 2000, 4000};
        List<String[]> rows = new ArrayList<>();
        List<LocalRedisRateLimiter> closables = new ArrayList<>();
        try {
            for (int w : windows) {
                // 固定超限场景：local-redis / burst=1.5 / nodes=4 / qps=2000>limit=1000
                Scenario sc = Scenario.local("window=" + w, 2000, false, 1.5, w, -1);
                BenchResult r = runOne(sc, closables);
                rows.add(new String[]{String.valueOf(w), String.valueOf(r.overLimitPct),
                        String.valueOf(r.redisPerSec), String.valueOf(r.avgUs), String.valueOf(r.p99Us)});
                printRow(sc.name, r);
            }
        } finally {
            closeAll(closables);
        }
        writeMarkdown(file("bench-results-window-ms.md"), rows, String[]::new,
                new String[]{"window(ms)", "overLimit%", "redis/s", "avg(us)", "p99(us)"});

        double w250 = Double.parseDouble(rows.get(0)[1]);
        double w4000 = Double.parseDouble(rows.get(rows.size() - 1)[1]);
        assertTrue(w4000 > w250, "窗口越大超限越重：4000(" + w4000 + "%) 应 > 250(" + w250 + "%)");
    }

    // ================= 工具 =================

    private static void closeAll(List<LocalRedisRateLimiter> closables) {
        for (LocalRedisRateLimiter c : closables) {
            c.close();
        }
    }

    private static double rowAvg(List<String[]> rows, String name, int col) {
        for (String[] r : rows) {
            if (r[0].equals(name)) {
                return Double.parseDouble(r[col]);
            }
        }
        return Double.MAX_VALUE;
    }

    private static String file(String name) {
        return "d:/project/chaos/chaos-java/jdk8-platform/jdk8-ratelimiter-demo/target/" + name;
    }

    private static void writeMarkdown(String path, List<String[]> rows,
                                      java.util.function.IntFunction<String[]> empty,
                                      String[] header) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# bench-results (SpringBootTest)\n\n");
        sb.append("> 由 mvn test 生成，Redis 8.0.2 @30102，8 线程。\n\n");
        for (int i = 0; i < header.length; i++) {
            sb.append("| ").append(header[i]);
        }
        sb.append(" |\n");
        for (int i = 0; i < header.length; i++) {
            sb.append("|---");
        }
        sb.append("|\n");
        for (String[] r : rows) {
            sb.append("| ");
            sb.append(String.join(" | ", r));
            sb.append(" |\n");
        }
        Files.write(Paths.get(path), sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}