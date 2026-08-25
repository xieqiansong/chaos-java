package lan.chaos.batchwriter.writer;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 基准方案：每个 item 即发一次 Redis 写命令（无批量、无队列缓冲）。
 * 命令数 = 条目数，网络往返随条目数线性放大，用于对照批量方案的收益。
 */
public final class LegacyRedisWriter implements BatchWriter<String> {

    private final StringRedisTemplate redis;
    private final String key;
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong calls = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();

    public LegacyRedisWriter(StringRedisTemplate redis, String key) {
        this.redis = redis;
        this.key = key;
    }

    @Override
    public String name() {
        return "legacy";
    }

    @Override
    public void write(String item) {
        calls.incrementAndGet();
        try {
            RedisBatchSupport.directHash(redis, key, item);
            written.incrementAndGet();
        } catch (RuntimeException e) {
            // Redis 抖动/超时：不中断汇聚线程，计数以便观察"逐条写入在抖动下的脆弱性"
            errors.incrementAndGet();
        }
    }

    @Override
    public long errors() {
        return errors.get();
    }

    @Override
    public long itemsWritten() {
        return written.get();
    }

    @Override
    public long redisCalls() {
        return calls.get();
    }

    @Override
    public double avgBatchSize() {
        return 1.0;
    }

    @Override
    public void start() {
        // 直写，无消费线程
    }

    @Override
    public void close() {
        // 直写，无需收尾
    }
}