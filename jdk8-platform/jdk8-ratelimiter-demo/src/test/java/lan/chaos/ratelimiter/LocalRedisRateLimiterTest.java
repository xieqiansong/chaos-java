package lan.chaos.ratelimiter;

import lan.chaos.ratelimiter.local.LocalRedisRateLimiter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地+Redis 双层限流的集成测试：需本地 Redis（默认 localhost:30102，与 seckill-demo 共用）。
 * 无 Redis 时经 Assumptions 优雅跳过，不影响无依赖用例与 CI。
 */
class LocalRedisRateLimiterTest {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 30102; // 与 seckill-demo 共用同一实例

    /** 真实密码从本项目 application-local.yml 读取（已被 gitignore，不提交）。 */
    private static String redisPassword() {
        try {
            List<String> ls = Files.readAllLines(
                    Paths.get("d:/project/chaos/chaos-java/jdk8-platform/jdk8-ratelimiter-demo/src/main/resources/application-local.yml"),
                    StandardCharsets.UTF_8);
            boolean inRedis = false;
            for (String raw : ls) {
                String t = raw.trim();
                if (t.equals("redis:")) {
                    inRedis = true;
                    continue;
                }
                if (inRedis) {
                    if (t.startsWith("password:")) {
                        return t.substring(t.indexOf(':') + 1).trim();
                    }
                    if (!raw.startsWith(" ") && !t.isEmpty()) {
                        inRedis = false;
                    }
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return System.getenv().getOrDefault("REDIS_PASSWORD", "redis123");
    }

    @Test
    void calibratesAndConvergesToGlobalQuota() throws Exception {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(REDIS_HOST, REDIS_PORT);
        cfg.setPassword(redisPassword());
        LettuceConnectionFactory factory = new LettuceConnectionFactory(cfg);
        factory.afterPropertiesSet();
        StringRedisTemplate redis = new StringRedisTemplate(factory);
        redis.afterPropertiesSet();
        try {
            // 探测 Redis，无则跳过
            try {
                redis.getConnectionFactory().getConnection().ping();
            } catch (Exception e) {
                Assumptions.assumeTrue(false, "无本地 Redis，跳过集成测试");
            }
            // 清理残留 key，保证用例可重复
            for (byte[] k : redis.getConnectionFactory().getConnection().keys("rl1:*".getBytes())) {
                redis.getConnectionFactory().getConnection().del(k);
            }
            for (byte[] k : redis.getConnectionFactory().getConnection().keys("rl2:*".getBytes())) {
                redis.getConnectionFactory().getConnection().del(k);
            }

            double globalQps = 100;
            int nodes = 2;
            long windowMs = 1000;
            LocalRedisRateLimiter l0 = new LocalRedisRateLimiter(redis, "n0", globalQps, windowMs, nodes, 1.0);
            LocalRedisRateLimiter l1 = new LocalRedisRateLimiter(redis, "n1", globalQps, windowMs, nodes, 1.0);
            try {
                // 预热 3 个窗口，让根 buckets 初始化、校准完成
                Thread.sleep(windowMs * 3);

                long deadline = System.nanoTime() + 2_000_000_000L;
                while (l0.tryAcquire("tenant-t") || l1.tryAcquire("tenant-t")) {
                    if (System.nanoTime() > deadline) {
                        break;
                    }
                }
                long allowed = l0.localAllows() + l1.localAllows();
                System.out.println("local-redis 窗口内实际放行 = " + allowed + "（全局限额 100/s）");
                // burst=1 时理论上界 = 全局每窗口配额（100），允许少量抖动
                assertTrue(allowed <= globalQps * 2, "burst=1 超限不应超过 2 倍预期，实际 " + allowed);
            } finally {
                l0.close();
                l1.close();
            }
        } finally {
            factory.destroy();
        }
    }
}