package lan.chaos.batchwriter.writer;

import lan.chaos.batchwriter.config.BatchWriterProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 按模式构造 Writer 的工厂，供压测场景复用。
 */
public final class BatchWriterFactory {

    private BatchWriterFactory() {
    }

    /**
     * @param mode legacy | static | adaptive
     * @param key  Redis Hash key（不同场景用独立 key 隔离，避免互相污染）
     */
    public static BatchWriter<String> create(String mode, StringRedisTemplate redis, String key, BatchWriterProperties p) {
        switch (mode) {
            case "legacy":
                return new LegacyRedisWriter(redis, key);
            case "static":
                return new StaticBatchWriter(redis, key, p.getStaticBatchSize(), p.getQueueCapacity(),
                        p.getIdleFlushMs(), p.getWriterThreads());
            case "adaptive":
            default:
                return new RedisAdaptiveBatchWriter(redis, key, p);
        }
    }
}