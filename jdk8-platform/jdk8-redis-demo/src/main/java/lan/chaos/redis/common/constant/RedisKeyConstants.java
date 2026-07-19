package lan.chaos.redis.common.constant;

/**
 * Redis Key 设计规范。
 *
 * <p>统一用 {@code 业务:模块:场景:} 前缀命名，避免不同业务 key 冲突，也便于按前缀批量删除/监控。
 * TTL（过期时间）在写入时按需指定，不要长期不失效占用内存。</p>
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /** 对象缓存：用户 */
    public static final String USER_KEY = "redis:demo:user:";
    /** 通用缓存前缀 */
    public static final String CACHE_KEY = "redis:demo:cache:";
    /** ZSet 排行榜 */
    public static final String RANK_KEY = "redis:demo:rank:game";
    /** 计数器前缀 */
    public static final String COUNTER_KEY = "redis:demo:counter:";
    /** 限流前缀 */
    public static final String RATE_LIMIT_KEY = "redis:demo:ratelimit:";
    /** 分布式锁前缀 */
    public static final String LOCK_KEY = "redis:demo:lock:";
    /** 发布订阅频道 */
    public static final String PUBSUB_CHANNEL = "redis:demo:channel";
}
