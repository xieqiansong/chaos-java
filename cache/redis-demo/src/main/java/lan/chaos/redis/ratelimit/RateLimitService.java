package lan.chaos.redis.ratelimit;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 限流（固定窗口）★★☆。
 *
 * <p><b>为什么用 Lua：</b>限流要判断「当前计数 + 是否超阈值」，两步操作需原子完成，
 * 否则并发下会出现「读到旧值→判断→写入」的竞态，导致限流失效。
 * 用 Lua 把 INCR + EXPIRE + 比较缩成一次原子执行。</p>
 *
 * <p><b>坑点：</b>固定窗口在窗口临界点会有「双倍突发」（如 0s 与 1s 各放满）。
 * 生产更常用滑动窗口 / 令牌桶（Redis-Cell、Gateway 内置限流或 Sentinel 兜底）。</p>
 */
@Service
public class RateLimitService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** KEYS[1]=限流key, ARGV[1]=窗口秒, ARGV[2]=最大次数；返回 1 放行 / 0 拦截 */
    private static final String RATE_LIMIT_SCRIPT =
            "local current = redis.call('incr', KEYS[1])\n" +
            "if current == 1 then redis.call('expire', KEYS[1], tonumber(ARGV[1])) end\n" +
            "if current > tonumber(ARGV[2]) then return 0 else return 1 end";

    public boolean tryAcquire(String key, int windowSeconds, int maxCount) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(RedisKeyConstants.RATE_LIMIT_KEY + key),
                String.valueOf(windowSeconds), String.valueOf(maxCount));
        return result != null && result == 1L;
    }
}
