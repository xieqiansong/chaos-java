package lan.chaos.demo.shortlink.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 * 作为 Redis 缓存的第一级防护，减少 Redis 压力
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("short-link");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 最大缓存条目数
                .maximumSize(100_000)
                // 写入后 5 分钟过期
                .expireAfterWrite(5, TimeUnit.MINUTES)
                // 初始预热大小
                .initialCapacity(10_000)
                .recordStats());
        return cacheManager;
    }
}
