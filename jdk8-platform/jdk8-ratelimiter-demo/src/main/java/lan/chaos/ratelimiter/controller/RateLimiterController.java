package lan.chaos.ratelimiter.controller;

import lan.chaos.ratelimiter.RateLimiter;
import lan.chaos.ratelimiter.config.RateLimiterProperties;
import lan.chaos.ratelimiter.local.LocalOnlyRateLimiter;
import lan.chaos.ratelimiter.local.LocalRedisRateLimiter;
import lan.chaos.ratelimiter.redis.RedisLuaRateLimiter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 限流演示接口：按 ratelimiter.mode 选实现，对指定租户执行 tryAcquire 并返回指标。
 */
@RestController
@RequestMapping("/api/ratelimiter")
public class RateLimiterController {

    private final RateLimiterProperties props;
    private final LocalRedisRateLimiter localRedis;
    private final RedisLuaRateLimiter redisLua;
    private final LocalOnlyRateLimiter localOnly;

    public RateLimiterController(RateLimiterProperties props,
                                 LocalRedisRateLimiter localRedis,
                                 RedisLuaRateLimiter redisLua,
                                 LocalOnlyRateLimiter localOnly) {
        this.props = props;
        this.localRedis = localRedis;
        this.redisLua = redisLua;
        this.localOnly = localOnly;
    }

    /** 取配置指定的默认实现。 */
    @GetMapping("/allow")
    public Map<String, Object> allow(@RequestParam(value = "tenant", defaultValue = "demo") String tenant) {
        RateLimiter limiter = resolve(props.getMode());
        boolean allowed = limiter.tryAcquire(tenant);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tenant", tenant);
        out.put("allowed", allowed);
        out.put("mode", limiter.name());
        out.put("limitQps", props.getDefaultQps());
        return out;
    }

    /** 指定实现执行一次，附带该实现的实时指标，便于观察 redis/s 与本地命中。 */
    @GetMapping("/stat")
    public Map<String, Object> stat(@RequestParam(value = "tenant", defaultValue = "demo") String tenant,
                                    @RequestParam(value = "mode", defaultValue = "local-redis") String mode) {
        RateLimiter limiter = resolve(mode);
        boolean allowed = limiter.tryAcquire(tenant);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tenant", tenant);
        out.put("allowed", allowed);
        out.put("mode", limiter.name());
        out.put("redisCalls", limiter.redisCalls());
        out.put("localAllows", limiter.localAllows());
        return out;
    }

    private RateLimiter resolve(String mode) {
        switch (mode) {
            case "redis-lua":
                return redisLua;
            case "local-only":
                return localOnly;
            case "local-redis":
            default:
                return localRedis;
        }
    }
}