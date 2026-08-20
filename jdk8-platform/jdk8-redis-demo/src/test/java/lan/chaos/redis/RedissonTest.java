package lan.chaos.redis;

import cn.hutool.core.lang.Console;
import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class RedissonTest {
    @Test
    public void test() {
        RedissonClient redissonClient = SpringUtil.getBean(RedissonClient.class);
        RLock lock = redissonClient.getLock("demo:redission:lock");
        lock.lock();
        try {
            Console.log("获取到锁，执行业务逻辑");
        } finally {
            Console.log("释放锁");
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
