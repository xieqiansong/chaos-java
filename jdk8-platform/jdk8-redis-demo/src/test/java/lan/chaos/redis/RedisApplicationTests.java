package lan.chaos.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文加载测试：验证所有 Bean（含拆分后的各 Service、PubSubConfig）装配无误。
 * 不需本地 Redis 即可通过（连接在使用时才建立）。
 */
@SpringBootTest
class RedisApplicationTests {

    @Test
    void contextLoads() {
    }
}
