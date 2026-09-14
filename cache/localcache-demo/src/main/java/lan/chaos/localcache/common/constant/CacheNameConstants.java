package lan.chaos.localcache.common.constant;

/** 缓存名集中管理，避免魔法值（Spring @Cacheable 的 cacheNames 引用这里）。 */
public final class CacheNameConstants {
    private CacheNameConstants() {}

    /** 声明式缓存（@Cacheable）使用的缓存名 */
    public static final String COMPUTE = "computeCache";
}
