package lan.chaos.localcache.eviction;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lan.chaos.localcache.common.model.User;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 能力三：容量淘汰（maximumSize）。
 *
 * <p>WHY：堆内缓存必须限制大小，否则无限增长 OOM。Caffeine 默认用 W-TinyLFU 淘汰策略，
 * 比传统 LRU 命中率更高（能识别「短暂突发」与「真正热点」）。
 *
 * <p>关键 API：maximumSize(n) 设上限；recordStats() 后可用 stats() 看命中率，
 * 用来验证「这个缓存有没有在帮你挡读」。
 * 生产坑：maximumSize 与 maximumWeight 二选一；统计 stats 有微小开销，按需开启。
 */
@Service
public class EvictionCacheService {

    private final Cache<Long, User> cache = Caffeine.newBuilder()
            .maximumSize(3)
            .recordStats()
            .build();

    public void put(Long id) {
        cache.put(id, User.sample(id));
    }

    public Cache<Long, User> cache() {
        return cache;
    }

    /** 控制台 / 测试统一入口：容量 3 却写入 5 个，必有淘汰；打印剩余 key 与命中率。 */
    public String run() {
        for (long i = 1; i <= 5; i++) {
            put(i);
        }
        // 触发一次淘汰维护，使 size 收敛到 maximumSize 以内。
        // 生产无需手动调：Caffeine 在后续读写时会自动做同样的维护。
        cache.cleanUp();
        StringBuilder sb = new StringBuilder();
        sb.append("maximumSize=3，写入 5 个 -> 存活 key=").append(cache.asMap().keySet())
                .append(" (已被淘汰到 ≤3)\n");
        sb.append("stats.hitRate=").append(String.format("%.2f", cache.stats().hitRate())).append('\n');
        return sb.toString();
    }
}
