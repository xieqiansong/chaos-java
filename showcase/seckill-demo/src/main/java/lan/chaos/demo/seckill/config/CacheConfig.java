package lan.chaos.demo.seckill.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 * 用于缓存商品信息等不频繁变更的数据，降低 Redis 压力
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("product");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 缓存最大条目数
                .maximumSize(100)
                // 写入后过期时间
                .expireAfterWrite(30, TimeUnit.SECONDS)
                // 记录统计信息
                .recordStats());
        return cacheManager;
    }
}
