package lan.chaos.multilevelcache.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 退化形态 L2：用本地 Map 模拟 Redis Hash + VERSION key。
 *
 * <p>仅用于「无 Redis 环境开箱即跑」的单进程演示。它完整复刻了
 * {@link RedisHashBackend} 的语义(含版本号自增)，因此版本号比对、双写、
 * L1 刷新等核心逻辑可被真实验证；但集群一致性只在同一进程内有效。
 */
public class InMemoryBackend implements CacheBackend {

    private final Map<String, Map<String, String>> store = new ConcurrentHashMap<>();
    private final Map<String, String> versions = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> getAll(String bizKey, String key) {
        return store.get(dataKey(bizKey, key));
    }

    @Override
    public void putAll(String bizKey, String key, Map<String, String> hash) {
        store.put(dataKey(bizKey, key), hash);
        incrementVersion(bizKey);
    }

    @Override
    public String getVersion(String bizKey) {
        return versions.get(versionKey(bizKey));
    }

    @Override
    public void setVersion(String bizKey, String version) {
        versions.put(versionKey(bizKey), version);
    }

    @Override
    public void remove(String bizKey, String key) {
        store.remove(dataKey(bizKey, key));
        incrementVersion(bizKey);
    }

    private synchronized void incrementVersion(String bizKey) {
        String k = versionKey(bizKey);
        String cur = versions.get(k);
        long next = (cur == null ? 0L : Long.parseLong(cur)) + 1L;
        versions.put(k, String.valueOf(next));
    }

    private String dataKey(String bizKey, String key) {
        return "DEMO:" + bizKey + ":" + key;
    }

    private String versionKey(String bizKey) {
        return "DEMO:" + bizKey + ":VERSION";
    }
}
