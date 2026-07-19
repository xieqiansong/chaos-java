package lan.chaos.redis.cache;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import lan.chaos.redis.common.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 字符串 / 对象缓存（String 结构）★★★。
 *
 * <p>最常用结构：缓存热点数据、对象 JSON、分布式会话等。重点掌握
 * {@code set/get/expire/ttl} 与带过期时间的写缓存。</p>
 */
@Service
public class StringCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 写入简单字符串并设置过期时间（秒） */
    public void setString(String key, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public String getString(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /** 缓存对象（JSON 序列化），默认 5 分钟过期 */
    public void cacheUser(User user) {
        redisTemplate.opsForValue().set(RedisKeyConstants.USER_KEY + user.getId(), user, 300, TimeUnit.SECONDS);
    }

    public Object getUser(Long id) {
        return redisTemplate.opsForValue().get(RedisKeyConstants.USER_KEY + id);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /** 查看剩余过期时间（秒）；-1 表示永不过期，-2 表示不存在 */
    public Long ttl(String key) {
        return redisTemplate.getExpire(key);
    }
}
