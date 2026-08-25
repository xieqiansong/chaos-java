package lan.chaos.ratelimiter.config;

import lan.chaos.ratelimiter.local.LocalOnlyRateLimiter;
import lan.chaos.ratelimiter.local.LocalRedisRateLimiter;
import lan.chaos.ratelimiter.redis.RedisLuaRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 三种限流实现统一装配成 Spring Bean（REST 接口按 ratelimiter.mode 选用）。
 * 生产常驻单节点：local-redis 的 nodeCount 取 1，单节点即可用满全局限额。
 */
@Configuration
public class RateLimiterConfig {

    @Bean(destroyMethod = "close")
    public LocalRedisRateLimiter localRedisRateLimiter(StringRedisTemplate redis,
                                                       RateLimiterProperties props) {
        return new LocalRedisRateLimiter(redis, "srv-local",
                props.getDefaultQps(), props.getWindowMs(), 1, props.getBurstMultiplier());
    }

    @Bean
    public RedisLuaRateLimiter redisLuaRateLimiter(StringRedisTemplate redis,
                                                   RateLimiterProperties props) {
        return new RedisLuaRateLimiter(redis, props.getDefaultQps(), props.getDefaultQps());
    }

    @Bean
    public LocalOnlyRateLimiter localOnlyRateLimiter(RateLimiterProperties props) {
        return new LocalOnlyRateLimiter(props.getDefaultQps(), props.getDefaultQps());
    }
}