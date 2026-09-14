package lan.chaos.batchwriter.bench;

import lan.chaos.batchwriter.config.BatchWriterProperties;
import lan.chaos.batchwriter.config.BenchProperties;
import lan.chaos.batchwriter.writer.BatchWriter;
import lan.chaos.batchwriter.writer.BatchWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 压测入口：--batchingest.bench.enabled=true 时，启动即对指定模式跑压力测试，
 * 打印吞吐 / Redis 命令量 / 平均批量后退出（便于一键拿到可对比的数据）。
 */
@Component
public class BenchRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchRunner.class);

    private final BenchProperties p;
    private final BatchWriterProperties props;
    private final StringRedisTemplate redis;
    private final ApplicationContext ctx;

    public BenchRunner(BenchProperties p, BatchWriterProperties props,
                       StringRedisTemplate redis, ApplicationContext ctx) {
        this.p = p;
        this.props = props;
        this.redis = redis;
        this.ctx = ctx;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!p.isEnabled()) {
            return;
        }
        BenchResult r;
        try {
            r = runOne(p.getMode(), p.getMode(), p.getThreads(), p.getRate(), p.getDurationSec(), p.isFlood());
        } finally {
            // nothing
        }
        log.info("=== {} {} ===", r.mode, p.toString());
        log.info("written={} items/s={} redisCalls={} redis/s={} avgBatch={} dropped={} errors={}",
                r.totalWritten, fmt(r.itemsPerSec), r.redisCalls, fmt(r.redisPerSec), fmt(r.avgBatchSize), r.dropped, r.errors);
        int exit = SpringApplication.exit(ctx, () -> 0);
        System.exit(exit);
    }

    private BenchResult runOne(String mode, String key, int threads, int rate, int durationSec, boolean flood) {
        BatchWriter<String> w = BatchWriterFactory.create(mode, redis, "bench:" + key, props);
        BenchOptions o = BenchOptions.of(threads, rate, durationSec, flood);
        try {
            return BenchEngine.run(w, o);
        } finally {
            w.close();
        }
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }
}