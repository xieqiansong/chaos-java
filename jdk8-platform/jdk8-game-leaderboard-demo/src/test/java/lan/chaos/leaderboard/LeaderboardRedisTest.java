package lan.chaos.leaderboard;

import lan.chaos.leaderboard.core.ZsetLeaderboardService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Redis 后端集成测试（{@code leaderboard.store=redis}）。
 *
 * <p>依赖本地 Redis；若环境无 Redis，{@link #assumeRedisUp()} 会让用例<b>跳过</b>而非失败，
 * 因此 {@code mvn test} 在无 Redis 的 CI 上也能通过。运行方式：</p>
 * <pre>
 *   docker run -d --name redis -p 6379:6379 redis:7.2
 *   mvn -pl jdk8-game-leaderboard-demo -am test -Dspring.profiles.active=redis
 * </pre>
 */
@SpringBootTest(properties = "leaderboard.store=redis")
class LeaderboardRedisTest {

    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ZsetLeaderboardService core;

    @BeforeEach
    void assumeRedisUp() {
        boolean up;
        try {
            String pong = redis.getConnectionFactory().getConnection().ping();
            up = pong != null && !pong.isEmpty();
        } catch (Exception e) {
            up = false;
        }
        Assumptions.assumeTrue(up, "本地 Redis 不可用，跳过 Redis 集成测试");
    }

    @Test
    void redis_zset_topN_desc() {
        core.setScore("low", 100);
        core.setScore("high", 200);
        assertEquals("high", core.topN(1).get(0).getMember());
        assertEquals(0L, core.rankOf("high"));
        assertEquals(1L, core.rankOf("low"));
    }
}
