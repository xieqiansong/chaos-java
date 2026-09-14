package lan.chaos.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Redis 演示启动类。
 *
 * <p>各 Redis 能力场景（缓存、Hash/List/Set、ZSet 排行榜、计数、限流、分布式锁、
 * Pub/Sub、Lua、Pipeline）按能力分包于 {@code lan.chaos.redis.<capability>}，
 * 由 {@code RedisScenarioTest} 等单元测试验证。</p>
 */
@SpringBootApplication
public class RedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisApplication.class, args);
    }
}
