package lan.chaos.batchwriter.writer;

import lan.chaos.batchwriter.config.BatchWriterProperties;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 自适应批量引擎的 Redis 落地实现：{@code storage} 用一次 Pipeline 批量写 Hash。
 */
public final class RedisAdaptiveBatchWriter extends AdaptiveBatchWriter<String> {

    private final StringRedisTemplate redis;
    private final String key;

    public RedisAdaptiveBatchWriter(StringRedisTemplate redis, String key, BatchWriterProperties p) {
        super(p);
        this.redis = redis;
        this.key = key;
    }

    @Override
    protected long storage(List<String> batch) {
        return RedisBatchSupport.pipelineHash(redis, key, batch);
    }
}