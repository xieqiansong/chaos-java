package lan.chaos.redis.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 序列化配置（◆ 基础）。
 *
 * <p>Spring Boot 默认的 {@code RedisTemplate} 用 JDK 序列化（二进制难读、跨语言差）。
 * 这里自定义一个 {@code RedisTemplate<String, Object>} 覆盖默认 Bean：</p>
 * <ul>
 *     <li><b>key / hashKey</b>：{@link StringRedisSerializer}，可读。</li>
 *     <li><b>value / hashValue</b>：{@link GenericJackson2JsonRedisSerializer}，
 *         写入 JSON 并带 {@code @class} 类型信息，读取时能正确反序列化为原 Java 对象（如 {@code User}）。</li>
 * </ul>
 * 简单字符串、计数、Pipeline、Lua 等场景继续使用 Spring Boot 自动配置的 {@code StringRedisTemplate}。
 * 连接信息见 {@code application.yml}（host/port/password/database + lettuce.pool）。
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // 带类型信息的 JSON 序列化器：写入时记录 @class，读取时还原为原类型
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
