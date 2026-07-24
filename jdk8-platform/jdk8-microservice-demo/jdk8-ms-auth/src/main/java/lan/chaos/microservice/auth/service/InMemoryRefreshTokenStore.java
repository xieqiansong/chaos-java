package lan.chaos.microservice.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刷新令牌的内存兜底实现。
 *
 * <p>由 {@code ms.security.refresh-store=memory} 启用。代价：重启即清空、多实例不共享。
 * 生产请改用 {@link RedisRefreshTokenStore}。</p>
 */
@Configuration
@ConditionalOnProperty(name = "ms.security.refresh-store", havingValue = "memory")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private static final String PREFIX = "auth:refresh:";

    private final Map<String, Boolean> store = new ConcurrentHashMap<>();

    private String key(Long userId, String jti) {
        return PREFIX + userId + ":" + jti;
    }

    @Override
    public void save(Long userId, String jti, long ttlSeconds) {
        store.put(key(userId, jti), Boolean.TRUE);
    }

    @Override
    public boolean exists(Long userId, String jti) {
        return Boolean.TRUE.equals(store.get(key(userId, jti)));
    }

    @Override
    public void removeAll(Long userId) {
        String prefix = PREFIX + userId + ":";
        Iterator<String> it = store.keySet().iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(prefix)) {
                it.remove();
            }
        }
    }
}
