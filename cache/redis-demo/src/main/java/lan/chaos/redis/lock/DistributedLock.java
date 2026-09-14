package lan.chaos.redis.lock;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁 ★★☆。
 *
 * <p>基于 {@code SET key value NX EX} 实现：只有 key 不存在时才能加锁成功（原子），
 * 并带过期时间防止死锁。释放锁必须用 Lua 脚本保证「只删自己加的锁」（校验 value 后删除），
 * 避免误删他人锁。{@link #withLock} 提供模板式用法。</p>
 */
@Service
public class DistributedLock {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /** 尝试加锁，成功返回 true */
    public boolean tryLock(String lockKey, String requestId, long expireSeconds) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.LOCK_KEY + lockKey, requestId, expireSeconds, TimeUnit.SECONDS));
    }

    /** 释放锁（仅删除自己持有的锁） */
    public boolean release(String lockKey, String requestId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(script,
                Collections.singletonList(RedisKeyConstants.LOCK_KEY + lockKey), requestId);
        return result != null && result == 1L;
    }

    /** 模板方法：加锁 -> 执行业务 -> 释放锁 */
    public <T> T withLock(String lockKey, long expireSeconds, Supplier<T> action) {
        String requestId = UUID.randomUUID().toString();
        if (!tryLock(lockKey, requestId, expireSeconds)) {
            throw new IllegalStateException("获取分布式锁失败: " + lockKey);
        }
        try {
            return action.get();
        } finally {
            release(lockKey, requestId);
        }
    }
}
