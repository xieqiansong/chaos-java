package lan.chaos.multilevelcache.cache;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 生产形态 L2：Redis Hash + 独立 VERSION key。
 *
 * <p>数据结构(车辆多级缓存实现采用相同结构)：
 * <pre>
 *   key  : DEMO:{bizKey}:{key}        ->  Hash(字段=实体属性, 值=属性值)
 *   ver  : DEMO:{bizKey}:VERSION       ->  String(整个 bizKey 的版本号, 数字字符串)
 * </pre>
 * 任一节点 put 时版本号自增，其他节点 get 时比对本地 L1 记录的版本号即可判断是否过期，
 * 无需逐字段比较，省去大量网络 IO 与反序列化。
 */
public class RedisHashBackend implements CacheBackend {

    private static final String KEY_PREFIX = "DEMO:";
    private static final String VERSION_SUFFIX = ":VERSION";
    private static final long VERSION_TTL_SECONDS = 3600L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, String> hashOps;

    public RedisHashBackend(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    private String dataKey(String bizKey, String key) {
        return KEY_PREFIX + bizKey + ":" + key;
    }

    private String versionKey(String bizKey) {
        return KEY_PREFIX + bizKey + VERSION_SUFFIX;
    }

    @Override
    public Map<String, String> getAll(String bizKey, String key) {
        return hashOps.entries(dataKey(bizKey, key));
    }

    @Override
    public void putAll(String bizKey, String key, Map<String, String> hash) {
        String dataKey = dataKey(bizKey, key);
        redisTemplate.delete(dataKey);
        hashOps.putAll(dataKey, hash);
        // 版本号自增，驱动其他节点 L1 失效
        Long newVersion = redisTemplate.opsForValue().increment(versionKey(bizKey));
        if (newVersion != null && newVersion == 1L) {
            redisTemplate.expire(versionKey(bizKey), VERSION_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public String getVersion(String bizKey) {
        Object v = redisTemplate.opsForValue().get(versionKey(bizKey));
        return v == null ? null : v.toString();
    }

    @Override
    public void setVersion(String bizKey, String version) {
        redisTemplate.opsForValue().set(versionKey(bizKey), version, VERSION_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void remove(String bizKey, String key) {
        redisTemplate.delete(dataKey(bizKey, key));
        redisTemplate.opsForValue().increment(versionKey(bizKey));
    }
}
