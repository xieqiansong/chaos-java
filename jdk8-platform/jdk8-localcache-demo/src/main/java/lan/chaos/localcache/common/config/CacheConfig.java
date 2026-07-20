package lan.chaos.localcache.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 声明式缓存（@Cacheable）所需的 CacheManager。
 *
 * <p>WHY：手动 Caffeine API 适合精确控制，但生产里 90% 的缓存场景只是
 * 「把方法结果缓存一下」，用 Spring 的 @Cacheable 声明式最省心。它需要一个 CacheManager，
 * 这里用 CaffeineCacheManager 把它接上。本配置只服务 cacheaside 场景，
 * 其余场景（basic/expire/eviction）各自用 Caffeine 原生 API，互不干扰。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES));
        return manager;
    }
}
