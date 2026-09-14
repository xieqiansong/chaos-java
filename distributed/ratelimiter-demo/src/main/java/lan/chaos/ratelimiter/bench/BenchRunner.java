package lan.chaos.ratelimiter.bench;

import lan.chaos.ratelimiter.RateLimiter;
import lan.chaos.ratelimiter.config.BenchProperties;
import lan.chaos.ratelimiter.local.LocalOnlyRateLimiter;
import lan.chaos.ratelimiter.local.LocalRedisRateLimiter;
import lan.chaos.ratelimiter.redis.RedisLuaRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 压测入口：--ratelimiter.bench.enabled=true 时，启动即对 N 个"节点"实例跑压力测试，
 * 打印吞吐/延迟/Redis 负载/超限率后退出（便于一键拿到可对比的数据）。
 */
@Component
public class BenchRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchRunner.class);

    private final BenchProperties p;
    private final StringRedisTemplate redis;
    private final ApplicationContext ctx;
    private final List<LocalRedisRateLimiter> closable = new ArrayList<>();

    public BenchRunner(BenchProperties p, StringRedisTemplate redis, ApplicationContext ctx) {
        this.p = p;
        this.redis = redis;
        this.ctx = ctx;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!p.isEnabled()) {
            return;
        }
        try {
            runBench();
        } finally {
            for (LocalRedisRateLimiter l : closable) {
                l.close();
            }
        }
        int exit = SpringApplication.exit(ctx, () -> 0);
        System.exit(exit);
    }

    private void runBench() {
        RateLimiter[] limiters = new RateLimiter[p.getNodes()];
        for (int i = 0; i < p.getNodes(); i++) {
            switch (p.getMode()) {
                case "redis-lua":
                    limiters[i] = new RedisLuaRateLimiter(redis, p.getLimit(), p.getLimit());
                    break;
                case "local-only":
                    limiters[i] = new LocalOnlyRateLimiter(p.getLimit(), p.getLimit());
                    break;
                case "local-redis":
                default:
                    LocalRedisRateLimiter l = new LocalRedisRateLimiter(redis, "n" + i,
                            p.getLimit(), p.getWindowMs(), p.getNodes(), p.getBurstMultiplier());
                    closable.add(l);
                    limiters[i] = l;
                    break;
            }
        }

        BenchOptions o = BenchOptions.of(p.getThreads(), p.getQps(), p.getLimit(), p.getDurationSec(),
                p.getNodes(), p.getBurstMultiplier(), p.getWindowMs(), p.isFlood(), p.getSkew());
        o.tenants = p.getTenants();
        BenchResult r = BenchEngine.run(limiters, o);

        log.info("=== {} {} ===", p.getMode(), o.describe());
        log.info("requests={} allowed={} denied={} qps={} avg={}us p99={}us",
                r.totalReq, r.allowed, r.denied, fmt(r.qps), fmt(r.avgUs), fmt(r.p99Us));
        log.info("redisCalls={} redis/s={} localAllows={} localHit%={} overLimit%={}",
                r.redisCalls, fmt(r.redisPerSec), r.localAllows,
                pct(r.localAllows, r.allowed), fmt(r.overLimitPct));
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static String pct(long part, long whole) {
        if (whole == 0) {
            return "0.00%";
        }
        return String.format("%.2f%%", part * 100.0 / whole);
    }
}