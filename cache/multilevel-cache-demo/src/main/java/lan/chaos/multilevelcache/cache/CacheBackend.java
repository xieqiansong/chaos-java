package lan.chaos.multilevelcache.cache;

import java.util.Map;

/**
 * L2 存储后端抽象。
 *
 * <p>典型实现中 L2 由 Redis Hash 承载，并使用一个独立的 VERSION key
 * 记录整个 bizKey 的版本号，用于实现「多节点间缓存一致性」。为让 Demo 在无 Redis
 * 的机器上也能开箱即跑，这里把 L2 抽象成接口：
 * <ul>
 *   <li>{@link RedisHashBackend}：生产形态，L2 = Redis Hash + VERSION key。</li>
 *   <li>{@link InMemoryBackend}：退化形态，用本地 Map 模拟 Redis，单进程演示版本号比对逻辑。</li>
 * </ul>
 */
public interface CacheBackend {

    /**
     * 从 L2 读取某个 key 的完整 Hash(字段-值映射)。
     *
     * @param bizKey 业务域，例如 VEHICLE
     * @param key    业务主键
     * @return 字段-值映射；L2 未命中返回 null
     */
    Map<String, String> getAll(String bizKey, String key);

    /**
     * 写入某个 key 的完整 Hash。
     */
    void putAll(String bizKey, String key, Map<String, String> hash);

    /**
     * 读取 bizKey 级别的版本号(字符串数字)。
     */
    String getVersion(String bizKey);

    /**
     * 设置 bizKey 级别的版本号。
     */
    void setVersion(String bizKey, String version);

    /**
     * 删除 L2 中某个 key(以及版本号)。
     */
    void remove(String bizKey, String key);
}
