package lan.chaos.multilevelcache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lan.chaos.multilevelcache.cache.CacheBackend;
import lan.chaos.multilevelcache.cache.InMemoryBackend;
import lan.chaos.multilevelcache.cache.MapRedisCacheable;
import lan.chaos.multilevelcache.cache.RedisHashBackend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 多级缓存装配：
 * <ul>
 *   <li>redis-enabled=true：L2 用 {@link RedisHashBackend}(真实 Redis Hash + VERSION)。</li>
 *   <li>redis-enabled=false：L2 用 {@link InMemoryBackend}(本地 Map 模拟，开箱即跑)。</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(MultilevelCacheProperties.class)
public class CacheAutoConfiguration {

    private final MultilevelCacheProperties props;

    public CacheAutoConfiguration(MultilevelCacheProperties props) {
        this.props = props;
    }

    @Bean
    @ConditionalOnProperty(name = "multilevel-cache.redis-enabled", havingValue = "true")
    public CacheBackend redisBackend(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return new RedisHashBackend(template);
    }

    @Bean
    @ConditionalOnProperty(name = "multilevel-cache.redis-enabled", havingValue = "false")
    public CacheBackend inMemoryBackend() {
        return new InMemoryBackend();
    }

    @Bean
    public MapRedisCacheable vehicleCache(CacheBackend backend) {
        MultilevelCacheProperties.Caffeine cf = props.getCaffeine();
        return new MapRedisCacheable(
                props.getDemo().getVehicleBizKey(),
                backend,
                cf.getMaximumSize(),
                cf.getExpireAfterWriteSeconds());
    }
}
