package lan.chaos.ratelimiter.redis;

import lan.chaos.ratelimiter.RateLimiter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基准方案：每请求一次 Redis+Lua 令牌桶，全局精确。
 * 多实例共享同一租户 key（rl1:{tenant}），等价于集群内全局精确限流。
 */
public final class RedisLuaRateLimiter implements RateLimiter {

    /** 令牌桶脚本见 resources/lua/token_bucket.lua。 */
    private static final DefaultRedisScript<String> SCRIPT = new DefaultRedisScript<>();
    static {
        SCRIPT.setLocation(new ClassPathResource("lua/token_bucket.lua"));
        SCRIPT.setResultType(String.class);
    }

    private final StringRedisTemplate redis;
    private final double ratePerSec;
    private final double capacity;
    private final AtomicLong redisCalls = new AtomicLong();

    public RedisLuaRateLimiter(StringRedisTemplate redis, double ratePerSec, double capacity) {
        this.redis = redis;
        this.ratePerSec = ratePerSec;
        this.capacity = capacity;
    }

    @Override
    public boolean tryAcquire(String tenantId) {
        redisCalls.incrementAndGet();
        String r = redis.execute(SCRIPT, Collections.singletonList("rl1:" + tenantId),
                String.valueOf(ratePerSec), String.valueOf(capacity),
                String.valueOf(System.currentTimeMillis()), "1");
        return "1".equals(r);
    }

    @Override
    public String name() {
        return "redis-lua";
    }

    @Override
    public long redisCalls() {
        return redisCalls.get();
    }

    @Override
    public long localAllows() {
        return 0;
    }
}