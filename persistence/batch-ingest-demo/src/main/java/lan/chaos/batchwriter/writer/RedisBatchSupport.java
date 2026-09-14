package lan.chaos.batchwriter.writer;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Redis 写入辅助：把一组 item 以一次 Pipeline 批量写入 Hash。
 *
 * <p>为什么用 Pipeline：把 N 条 {@code HSET} 命令合并为一次网络往返，命令数从 N 降到 1，
 * 是批量入库降低 Redis 负载的核心收益。
 */
public final class RedisBatchSupport {

    private RedisBatchSupport() {
    }

    /**
     * 批量写 Hash（一次 Pipeline 往返）。
     *
     * @return 该批次耗时（纳秒）
     */
    public static long pipelineHash(StringRedisTemplate redis, String key, List<String> items) {
        RedisConnectionFactory factory = redis.getConnectionFactory();
        if (factory == null) {
            throw new IllegalStateException("RedisConnectionFactory is null");
        }
        long t0 = System.nanoTime();
        redis.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) conn -> {
            byte[] hashKey = key.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < items.size(); i++) {
                byte[] field = ("f" + i).getBytes(StandardCharsets.UTF_8);
                byte[] value = items.get(i).getBytes(StandardCharsets.UTF_8);
                conn.hashCommands().hSet(hashKey, field, value);
            }
            return null;
        });
        return System.nanoTime() - t0;
    }

    /** 单条写 Hash（一次命令 = 一次往返）。 */
    public static void directHash(StringRedisTemplate redis, String key, String item) {
        // 字段必须显式转 String：StringRedisTemplate 的 hashKey 序列化器是 StringRedisSerializer，
        // 传 Long(如 System.nanoTime()) 会直接 ClassCastException
        redis.opsForHash().put(key, String.valueOf(System.nanoTime()), item);
    }
}