package lan.chaos.leaderboard.common.store;

import lan.chaos.leaderboard.common.constant.LeaderboardConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * ScoreBoard 工厂：根据 {@code leaderboard.store} 决定后端。
 *
 * <ul>
 *     <li>{@code memory}（默认）：返回 {@link MemoryScoreBoard}，零外部依赖，可直接跑 DemoApp / 单测。</li>
 *     <li>{@code redis}：返回 {@link RedisScoreBoard}，使用真实 Redis ZSET。</li>
 * </ul>
 * 每个能力包调用 {@link #create(String)} 拿到<b>独立命名</b>的 board，彼此隔离。
 */
@Component
public class ScoreBoardFactory {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${leaderboard.store:memory}")
    private String store;

    public ScoreBoard create(String name) {
        if ("redis".equalsIgnoreCase(store) && redisTemplate != null) {
            return new RedisScoreBoard(redisTemplate, LeaderboardConstants.boardKey(name));
        }
        return new MemoryScoreBoard(name);
    }
}
