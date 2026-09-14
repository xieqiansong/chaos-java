package lan.chaos.leaderboard.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lan.chaos.leaderboard.common.model.RankEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置（◆ 基础）。
 *
 * <p>为「双层缓存」场景提供 Caffeine 容器：本地缓存 Redis ZSET 算出的 Top100，
 * 避免每个请求都打 Redis。这里用 {@code expireAfterWrite=300s} 的被动过期 +
 * 业务侧主动 {@code refresh()}（对应面经伪代码的定时刷新），与文档方案一致。</p>
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, List<RankEntry>> topBoardCache() {
        return Caffeine.newBuilder()
                .maximumSize(16)
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }
}
