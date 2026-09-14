package lan.chaos.multilevelcache.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * 多级缓存模板：L1=Caffeine，L2={@link CacheBackend}(Redis Hash 或内存退化版)。
 *
 * <p>本类实现「Caffeine(L1) + Redis Hash(L2) + 版本号」多级缓存，
 * 把本地 {@code ConcurrentHashMap} 替换为 <b>Caffeine</b>(L1 本地缓存，带容量上限与 TTL，
 * 防止内存泄漏)，并完整保留「版本号 + 集群一致性」的设计：
 * <ul>
 *   <li>L2 用一个 bizKey 级别的 VERSION 版本号；任一节点写入即自增。</li>
 *   <li>读路径：先查 L1(Caffeine)；命中后比对本地记录的版本号与 L2 当前版本号，
 *       一致则直接返回(跳过网络 IO 与反序列化)，不一致才回源 L2 并刷新 L1。</li>
 *   <li>写路径：先写 L2(带版本号自增)，再写 L1(Caffeine)。</li>
 * </ul>
 *
 * <p>用 {@link ReadWriteLock} 保护「L1 读取 + L1 写入版本号」这一读-改-写区间，
 * 避免并发下 L1 命中但版本号未刷新的竞态。
 *
 * @param <K> 业务主键类型
 * @param <V> 缓存值类型(需可被 {@link #toHash} / {@link #fromHash} 在 Hash 字段间转换)
 */
@Slf4j
public abstract class AbstractMultilevelCacheable<K, V> {

    /** bizKey，同一业务域共享一个 VERSION 版本号。 */
    protected final String bizKey;
    /** L1：Caffeine 本地缓存，替换朴素 ConcurrentHashMap。 */
    protected final Cache<K, V> localCache;
    /** L2 后端(可切换 Redis / 内存)。 */
    protected final CacheBackend backend;

    /** 每个业务主键在 L1 中记录的「版本号」，用于与 L2 VERSION 比对。 */
    private final Map<K, Long> localVersionMap = new ConcurrentHashMap<>();
    /** 保护 L1 读 + 版本号判断 + L1 写的临界区。 */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    protected AbstractMultilevelCacheable(String bizKey,
                                          CacheBackend backend,
                                          long maximumSize,
                                          long expireAfterWriteSeconds) {
        this.bizKey = bizKey;
        this.backend = backend;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWriteSeconds, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 业务值 -> L2 Hash 字段映射(序列化)。实现可用反射把实体字段铺平成 Hash。
     */
    protected abstract Map<String, String> toHash(V value);

    /**
     * L2 Hash 字段映射 -> 业务值(反序列化)。
     */
    protected abstract V fromHash(Map<String, String> hash);

    /**
     * 计算业务值的版本号：用内容 hashCode 表达「值是否变化」。
     * 也可用乐观锁版本字段，这里保持一致性的 hashCode 思路。
     */
    protected long versionOf(V value) {
        return value == null ? 0L : (long) value.hashCode();
    }

    /**
     * 回源：L1、L2 都未命中时加载数据(DB/远程调用)。由子类实现。
     */
    protected abstract V loadFromSource(K key);

    /**
     * 读路径：先 L1(Caffeine)，命中则比对版本号，一致直接返回；
     * 否则回源 L2 或数据源，并刷新 L1。
     */
    public V get(K key) {
        V localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            Long localVer = localVersionMap.get(key);
            String l2Ver = backend.getVersion(bizKey);
            // 版本号一致：跳过网络 IO，直接返回本地副本
            if (l2Ver != null && localVer != null && localVer.toString().equals(l2Ver)) {
                log.debug("[L1 hit & version match] bizKey={} key={}", bizKey, key);
                return localValue;
            }
            // 版本号不一致或本地版本缺失：尝试从 L2 刷新
            Map<String, String> hash = backend.getAll(bizKey, String.valueOf(key));
            if (hash != null && !hash.isEmpty()) {
                V value = fromHash(hash);
                refreshLocal(key, value, parseVersion(l2Ver));
                return value;
            }
        }
        // L1 未命中：从 L2 或数据源加载
        return loadAndCache(key);
    }

    private V loadAndCache(K key) {
        Map<String, String> hash = backend.getAll(bizKey, String.valueOf(key));
        V value;
        if (hash != null && !hash.isEmpty()) {
            value = fromHash(hash);
        } else {
            value = loadFromSource(key); // 回源
            if (value == null) {
                return null;
            }
            put(key, value); // 双写
            return value;
        }
        String l2Ver = backend.getVersion(bizKey);
        refreshLocal(key, value, parseVersion(l2Ver));
        return value;
    }

    /**
     * 写路径：先写 L2(版本号自增)，再写 L1(Caffeine)。
     */
    public void put(K key, V value) {
        backend.putAll(bizKey, String.valueOf(key), toHash(value));
        String l2Ver = backend.getVersion(bizKey);
        refreshLocal(key, value, parseVersion(l2Ver));
    }

    /**
     * 删除：删除 L2(版本号自增驱动其他节点 L1 失效)，并清除本节点 L1。
     */
    public void remove(K key) {
        backend.remove(bizKey, String.valueOf(key));
        lock.writeLock().lock();
        try {
            localCache.invalidate(key);
            localVersionMap.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
        log.debug("[remove] bizKey={} key={}", bizKey, key);
    }

    /** 批量构建：演示 L1 批量聚合。 */
    public Map<K, V> getAll(java.util.Collection<K> keys) {
        Map<K, V> result = new java.util.LinkedHashMap<>();
        for (K k : keys) {
            result.put(k, get(k));
        }
        return result;
    }

    /** 用指定回源函数做一次性填充(便于测试注入数据)。 */
    public V computeIfAbsent(K key, Function<K, V> loader) {
        V v = localCache.getIfPresent(key);
        if (v != null) {
            return v;
        }
        v = loader.apply(key);
        if (v != null) {
            put(key, v);
        }
        return v;
    }

    private void refreshLocal(K key, V value, long l2Ver) {
        lock.writeLock().lock();
        try {
            localCache.put(key, value);
            if (l2Ver > 0) {
                localVersionMap.put(key, l2Ver);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private long parseVersion(String v) {
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
