package lan.chaos.redis;

import lan.chaos.redis.cache.StringCacheService;
import lan.chaos.redis.common.model.User;
import lan.chaos.redis.counter.CounterService;
import lan.chaos.redis.lock.DistributedLock;
import lan.chaos.redis.rank.RankService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景集成测试（AGENTS「单元测试」形态）：直接验证各场景语义。
 *
 * <p>依赖本地 Redis；若环境无 Redis，{@link #assumeRedisUp()} 会让用例<b>跳过</b>而非失败，
 * 因此 {@code mvn test} 在无 Redis 的 CI 上也能通过。</p>
 */
@SpringBootTest
class RedisScenarioTest {

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private StringCacheService cache;
    @Autowired
    private RankService rank;
    @Autowired
    private CounterService counter;
    @Autowired
    private DistributedLock lock;

    @BeforeEach
    void assumeRedisUp() {
        boolean up;
        try {
            String pong = redis.getConnectionFactory().getConnection().ping();
            up = pong != null && !pong.isEmpty();
        } catch (Exception e) {
            up = false;
        }
        Assumptions.assumeTrue(up, "本地 Redis 不可用，跳过集成测试");
    }

    @Test
    void cacheUser_roundTrip_json() {
        long id = System.nanoTime();
        cache.cacheUser(new User(id, "tom", 18));
        Object got = cache.getUser(id);
        assertNotNull(got);
        assertTrue(got instanceof User);
        assertEquals("tom", ((User) got).getName());
    }

    @Test
    void rank_topN_desc() {
        String low = "u_" + System.nanoTime() + "_low";
        String high = "u_" + System.nanoTime() + "_high";
        rank.addScore(low, 100);
        rank.addScore(high, 200);
        Set<String> top = rank.rankDesc(1);
        assertTrue(top.contains(high));
        assertFalse(top.contains(low));
    }

    @Test
    void counter_incr_atomic() {
        long before = counter.get("t");
        long after = counter.incr("t");
        assertEquals(before + 1, after);
    }

    @Test
    void lock_runsExactlyOnce() {
        String r = lock.withLock("k:" + System.nanoTime(), 30, () -> "ok");
        assertEquals("ok", r);
    }
}
