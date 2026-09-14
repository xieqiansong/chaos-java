package lan.chaos.microservice.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 刷新令牌的 Redis 实现（生产推荐，默认启用）。
 *
 * <p>key 设计：{@code auth:refresh:{userId}:{jti}}，value 占位 "1"，TTL 与 refresh token 一致。
 * 退出登录按前缀 {@code auth:refresh:{userId}:*} 删除即可踢下线。</p>
 *
 * <p>由 {@code ms.security.refresh-store=redis}（默认）启用；设为 {@code memory} 时走内存兜底。
 * 注意这里只用 @ConditionalOnProperty（不依赖 bean 存在性），规避 @ConditionalOnBean 的顺序坑。</p>
 */
@Configuration
@ConditionalOnProperty(name = "ms.security.refresh-store", havingValue = "redis", matchIfMissing = true)
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(Long userId, String jti) {
        return PREFIX + userId + ":" + jti;
    }

    @Override
    public void save(Long userId, String jti, long ttlSeconds) {
        redisTemplate.opsForValue().set(key(userId, jti), "1", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean exists(Long userId, String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(userId, jti)));
    }

    @Override
    public void removeAll(Long userId) {
        String pattern = PREFIX + userId + ":*";
        // demo 规模小，直接用 keys 做前缀扫描删除；生产建议用 SCAN 避免阻塞
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}
