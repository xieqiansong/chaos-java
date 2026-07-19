package lan.chaos.redis.counter;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 计数 / 自增（String + INCR）★★★。
 *
 * <p>{@code INCR} 是原子操作，单线程 Redis 保证并发安全，常用于点赞数、访问量、库存计数、
 * 分布式 ID 生成等。注意 value 为字符串形式的整数。</p>
 *
 * <p><b>坑点：</b>计数与业务逻辑需各自保证一致（如先 INCR 再落库失败要回补）；
 * value 超过 Long 范围会溢出；高并发计数建议分片（如 key + hash 取模）避免热点。</p>
 */
@Service
public class CounterService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public Long incr(String key) {
        return stringRedisTemplate.opsForValue().increment(RedisKeyConstants.COUNTER_KEY + key);
    }

    public Long incrBy(String key, long delta) {
        return stringRedisTemplate.opsForValue().increment(RedisKeyConstants.COUNTER_KEY + key, delta);
    }

    public Long decr(String key) {
        return stringRedisTemplate.opsForValue().decrement(RedisKeyConstants.COUNTER_KEY + key);
    }

    public Long get(String key) {
        String v = stringRedisTemplate.opsForValue().get(RedisKeyConstants.COUNTER_KEY + key);
        return (v == null || v.trim().isEmpty()) ? 0L : Long.parseLong(v);
    }
}
