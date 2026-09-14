package lan.chaos.localcache.expire;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lan.chaos.localcache.common.model.User;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 能力二：写入过期（TTL）。
 *
 * <p>WHY：本地缓存最怕「脏数据一直不更新」。expireAfterWrite 让写入后经过固定时间自动失效，
 * 下次读取拿不到（或走加载逻辑拿新值），实现「最终一致」。
 *
 * <p>关键 API：expireAfterWrite(duration)。还有 expireAfterAccess（空闲过期）、
 * expireAfter（自定义读写后过期，最灵活）。
 * 生产坑：TTL 不要全设一样，否则同时失效引发「缓存雪崩」，应加随机抖动。
 */
@Service
public class ExpireCacheService {

    // 演示用 2 秒短 TTL；生产通常是分钟~小时级
    private final Cache<Long, User> cache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public void put(Long id) {
        cache.put(id, User.sample(id));
    }

    public User get(Long id) {
        return cache.getIfPresent(id);
    }

    /** 控制台 / 测试统一入口：写入后立刻能取到，过期后取不到。 */
    public String run() {
        put(1L);
        StringBuilder sb = new StringBuilder();
        sb.append("put(1) 后立刻取 -> ").append(get(1L)).append("  (未过期)\n");
        try {
            Thread.sleep(2100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sb.append("等 2.1s 后再取   -> ").append(get(1L)).append("  (已过期自动失效)\n");
        return sb.toString();
    }
}
