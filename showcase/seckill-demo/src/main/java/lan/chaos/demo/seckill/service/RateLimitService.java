package lan.chaos.demo.seckill.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 令牌桶限流服务
 * <p>
 * 基于 Redis 实现分布式令牌桶算法，支持秒级 QPS 控制。
 * 每个商品独立限流，避免单一热点商品耗尽系统资源。
 */
@Service
public class RateLimitService {

    private static final String RATE_LIMIT_KEY_PREFIX = "seckill:rate_limit:";

    @Value("${seckill.rate-limit.default-qps:1000}")
    private int defaultQps;

    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取令牌
     *
     * @param productId 商品 ID
     * @return true=允许通过，false=限流
     */
    public boolean tryAcquire(Long productId) {
        return tryAcquire(productId, defaultQps);
    }

    /**
     * 尝试获取令牌（指定 QPS）
     *
     * @param productId 商品 ID
     * @param qps       每秒允许的请求数
     * @return true=允许通过，false=限流
     */
    public boolean tryAcquire(Long productId, int qps) {
        String key = RATE_LIMIT_KEY_PREFIX + productId;
        long now = System.currentTimeMillis();
        long windowMs = 1000L; // 1秒时间窗口

        // 使用 Redis INCR 实现滑动窗口计数
        // key 精确到秒级：seckill:rate_limit:{productId}:{seconds}
        String secondKey = key + ":" + (now / 1000);

        Long count = stringRedisTemplate.opsForValue().increment(secondKey);
        if (count != null && count == 1) {
            // 首次设置，1秒后自动过期
            stringRedisTemplate.expire(secondKey, 2, TimeUnit.SECONDS);
        }

        return count != null && count <= qps;
    }

    /**
     * 获取当前 QPS 统计
     */
    public long getCurrentQps(Long productId) {
        String key = RATE_LIMIT_KEY_PREFIX + productId + ":" + (System.currentTimeMillis() / 1000);
        String val = stringRedisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val) : 0;
    }
}
