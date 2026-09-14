package lan.chaos.redis.rank;

import lan.chaos.redis.common.constant.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 有序集合（ZSet）排行榜 ★★★。
 *
 * <p>每个 member 关联一个 score，Redis 自动按 score 排序，天然适合游戏积分、销量、热度排行。
 * 常用 {@code add / incrementScore / reverseRange（倒序取TopN）/ reverseRank（名次）/ score}。</p>
 *
 * <p><b>坑点：</b>member 必须是唯一字符串（同名会覆盖分数）；并列排名时
 * {@code reverseRange} 按区间取可能截断同分者；score 为 double，超大整数有精度风险。</p>
 */
@Service
public class RankService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public Boolean addScore(String member, double score) {
        return stringRedisTemplate.opsForZSet().add(RedisKeyConstants.RANK_KEY, member, score);
    }

    /** 原子增减分数（如点赞数、积分变化） */
    public Double incrScore(String member, double delta) {
        return stringRedisTemplate.opsForZSet().incrementScore(RedisKeyConstants.RANK_KEY, member, delta);
    }

    public Double getScore(String member) {
        return stringRedisTemplate.opsForZSet().score(RedisKeyConstants.RANK_KEY, member);
    }

    /** 倒序取 TopN（score 高者在前） */
    public Set<String> rankDesc(long topN) {
        return stringRedisTemplate.opsForZSet().reverseRange(RedisKeyConstants.RANK_KEY, 0, topN - 1);
    }

    /** 带分数的 TopN 结果 */
    public Set<ZSetOperations.TypedTuple<String>> rankWithScore(long topN) {
        return stringRedisTemplate.opsForZSet().reverseRangeWithScores(RedisKeyConstants.RANK_KEY, 0, topN - 1);
    }

    /** 某成员的名次（从 0 开始，0 为第一名） */
    public Long rankOf(String member) {
        return stringRedisTemplate.opsForZSet().reverseRank(RedisKeyConstants.RANK_KEY, member);
    }

    public Long remove(String member) {
        return stringRedisTemplate.opsForZSet().remove(RedisKeyConstants.RANK_KEY, member);
    }
}
