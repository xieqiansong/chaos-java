package lan.chaos.leaderboard.doublecache;

import com.github.benmanes.caffeine.cache.Cache;
import lan.chaos.leaderboard.common.constant.LeaderboardConstants;
import lan.chaos.leaderboard.common.model.RankEntry;
import lan.chaos.leaderboard.common.store.ScoreBoard;
import lan.chaos.leaderboard.common.store.ScoreBoardFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * ② 双层缓存 Top100（对应面经「Top K 查询优化 - 双层缓存策略」）。
 *
 * <p>本地 Caffeine 缓存「由 Redis ZSET 算出的 Top100」，查询优先走本地缓存，
 * 命中即返回（亚毫秒级），未命中才回源 ScoreBoard。对应文档伪代码：
 * 定时/主动 {@link #refresh()} 重新拉取并写回本地缓存，避免每次请求打 Redis。</p>
 *
 * <p>这是「本地缓存(Redis 之上) + 分布式存储」的双层结构：本地挡热点、Redis 挡共享。</p>
 */
@Service
public class TopBoardCacheService {

    private final ScoreBoard board;
    private final Cache<String, List<RankEntry>> cache;
    private final int topN;

    @Autowired
    public TopBoardCacheService(ScoreBoardFactory factory,
                                Cache<String, List<RankEntry>> cache,
                                @Value("${leaderboard.topn:100}") int topN) {
        this.board = factory.create("doublecache");
        this.cache = cache;
        this.topN = topN;
    }

    /** 读 Top100：命中本地缓存直接返回，未命中回源 ScoreBoard 并写入缓存 */
    public List<RankEntry> getTop() {
        return cache.get(LeaderboardConstants.CACHE_TOP100_KEY, k -> board.topN(topN));
    }

    /** 更新积分（同时写底层 ScoreBoard；缓存未刷新前 Top100 仍命中旧值） */
    public void updateScore(String userId, double delta) {
        board.incrementScore(userId, delta);
    }

    /** 主动刷新（对应文档 refresh_top100：重新拉取并覆盖缓存） */
    public void refresh() {
        cache.invalidateAll();
        getTop();
    }

    /** 本地缓存命中率（0~1），便于观察双层缓存是否生效 */
    public double hitRate() {
        return cache.stats().hitRate();
    }

    public String run() {
        board.clear();
        // 200 个用户，分数 1..200（u1 最低、u200 最高）
        for (int i = 1; i <= 200; i++) {
            updateScore("u" + i, i);
        }
        StringJoiner sb = new StringJoiner("\n");
        List<RankEntry> first = getTop();
        sb.add("首次 getTop() -> " + first.get(0) + " ... " + first.get(first.size() - 1));

        // 把一个低分用户刷成最高，验证「缓存未刷新前仍返回旧 Top100」
        board.setScore("u1", 999999);
        List<RankEntry> cached = getTop();
        sb.add("u1 刷成最高分后(未刷新) getTop()[0] -> " + cached.get(0) + "（仍命中旧缓存）");

        // 主动刷新后，Top100 立即反映最新
        refresh();
        List<RankEntry> fresh = getTop();
        sb.add("refresh() 后 getTop()[0] -> " + fresh.get(0) + "（已是最新）");
        sb.add("缓存命中统计: hitRate=" + String.format("%.2f", cache.stats().hitRate()));
        return sb.toString() + "\n";
    }
}
