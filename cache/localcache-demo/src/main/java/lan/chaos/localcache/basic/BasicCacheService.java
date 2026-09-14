package lan.chaos.localcache.basic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lan.chaos.localcache.common.model.User;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 能力一：基础读写。
 *
 * <p>WHY：本地缓存把热点数据放 JVM 堆内，读延迟纳秒~微秒级，远快于 Redis（毫秒级网络往返）
 * 和数据库。适合「读多写少、允许短暂不一致」的场景：配置项、热点用户、字典表。
 *
 * <p>关键 API：put / getIfPresent（不触发加载）/ invalidate（主动失效）。
 * 生产坑：堆内缓存会吃内存，必须设 maximumSize 或 expireAfterWrite 做兜底，否则 OOM。
 */
@Service
public class BasicCacheService {

    private final Cache<Long, User> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .build();

    /** 写入并返回刚存入的对象（getIfPresent 不会触发加载）。 */
    public User putAndGet(Long id) {
        User user = User.sample(id);
        cache.put(id, user);
        return cache.getIfPresent(id);
    }

    /** 读取（不触发任何加载逻辑）。 */
    public User get(Long id) {
        return cache.getIfPresent(id);
    }

    /** 主动失效：后续 getIfPresent 返回 null。 */
    public void invalidate(Long id) {
        cache.invalidate(id);
    }

    /** 控制台 / 测试统一入口：返回「输入→输出」可读结果。 */
    public String run() {
        StringBuilder sb = new StringBuilder();
        User put = putAndGet(1L);
        sb.append("put(1)         -> ").append(put).append('\n');
        sb.append("getIfPresent(1) -> ").append(get(1L)).append("  (命中)\n");
        invalidate(1L);
        sb.append("invalidate(1)   -> ").append(get(1L)).append("  (已失效)\n");
        return sb.toString();
    }
}
