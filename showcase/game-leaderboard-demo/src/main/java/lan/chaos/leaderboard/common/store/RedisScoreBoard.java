package lan.chaos.leaderboard.common.store;

import lan.chaos.leaderboard.common.model.RankEntry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 基于 Redis ZSET 的 ScoreBoard 实现（生产真实后端，{@code store=redis}）。
 *
 * <p>Redis ZSET 内部为「跳跃表 + 哈希表」：
 * <ul>
 *     <li>更新积分 {@code ZADD} / {@code ZINCRBY}：O(log N)</li>
 *     <li>查询 Top K {@code ZREVRANGE ... WITHSCORES}：O(log N + K)</li>
 * </ul>
 * 百万用户约 50MB 内存，Top100 查询稳定 2ms 内（与面经方案一致）。</p>
 */
public class RedisScoreBoard implements ScoreBoard {

    private final StringRedisTemplate redis;
    private final String key;

    public RedisScoreBoard(StringRedisTemplate redis, String key) {
        this.redis = redis;
        this.key = key;
    }

    @Override
    public void setScore(String member, double score) {
        redis.opsForZSet().add(key, member, score);
    }

    @Override
    public double incrementScore(String member, double delta) {
        Double after = redis.opsForZSet().incrementScore(key, member, delta);
        return after == null ? delta : after;
    }

    @Override
    public Double getScore(String member) {
        return redis.opsForZSet().score(key, member);
    }

    @Override
    public List<RankEntry> topDesc(long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(key, start, end);
        List<RankEntry> result = new ArrayList<>();
        if (tuples == null) {
            return result;
        }
        long rank = start;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            if (t.getValue() == null || t.getScore() == null) {
                continue;
            }
            result.add(new RankEntry(t.getValue(), t.getScore(), rank++));
        }
        return result;
    }

    @Override
    public Long rankOf(String member) {
        return redis.opsForZSet().reverseRank(key, member);
    }

    @Override
    public long size() {
        Long c = redis.opsForZSet().zCard(key);
        return c == null ? 0 : c;
    }

    @Override
    public void remove(String member) {
        redis.opsForZSet().remove(key, member);
    }

    @Override
    public void clear() {
        redis.delete(key);
    }

    @Override
    public String toString() {
        return "RedisScoreBoard(key=" + key + ")";
    }
}
