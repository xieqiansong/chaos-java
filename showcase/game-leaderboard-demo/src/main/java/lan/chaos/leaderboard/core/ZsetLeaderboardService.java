package lan.chaos.leaderboard.core;

import lan.chaos.leaderboard.common.model.RankEntry;
import lan.chaos.leaderboard.common.store.ScoreBoard;
import lan.chaos.leaderboard.common.store.ScoreBoardFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * ① 核心 ZSET 排行榜（★ 架构基石，对应面经「核心数据结构选择」）。
 *
 * <p>直接将用户积分写入 {@link ScoreBoard}（Redis ZSET / 内存）：
 * 更新 {@code ZADD/ZINCRBY} 为 O(log N)，Top K 查询 {@code ZREVRANGE} 为 O(log N + K)。
 * 在 core 之上再叠加双层缓存、热数据、分片、异步流水线四层优化。</p>
 */
@Service
public class ZsetLeaderboardService {

    private final ScoreBoard board;
    private final int topN;

    @Autowired
    public ZsetLeaderboardService(ScoreBoardFactory factory,
                                   @Value("${leaderboard.topn:100}") int topN) {
        this.board = factory.create("core");
        this.topN = topN;
    }

    /** 原子增减积分（游戏里多为加分），返回变更后积分 */
    public double updateScore(String userId, double delta) {
        return board.incrementScore(userId, delta);
    }

    /** 直接设置积分（覆盖） */
    public void setScore(String userId, double score) {
        board.setScore(userId, score);
    }

    /** 取积分 */
    public Double scoreOf(String userId) {
        return board.getScore(userId);
    }

    /** 倒序 Top N（高分在前） */
    public List<RankEntry> topN(int n) {
        return board.topDesc(0, n - 1);
    }

    /** 某用户名次（0 基，0=第一名）；不在榜返回 null */
    public Long rankOf(String userId) {
        return board.rankOf(userId);
    }

    /**
     * 查询「某用户附近」的名次区间（如本人前后各 2 名）。
     * 百万用户查自己排名时，避免全量拉取，只取一个小窗口即可。
     */
    public List<RankEntry> rankAround(String userId, int radius) {
        Long rank = board.rankOf(userId);
        if (rank == null) {
            return java.util.Collections.emptyList();
        }
        long start = Math.max(0, rank - radius);
        long end = rank + radius;
        return board.topDesc(start, end);
    }

    /** 控制台演示：输入 → 输出 */
    public String run() {
        board.clear();
        // 种子数据：10 个用户，分数 100~1000 递减
        for (int i = 1; i <= 10; i++) {
            updateScore("u" + i, i * 100.0);
        }
        StringJoiner sb = new StringJoiner("\n");
        sb.add("updateScore(u5, +500) -> " + updateScore("u5", 500));
        sb.add("topN(" + Math.min(5, topN) + "): " + topN(5));
        sb.add("rankOf(u5) -> #" + (rankOf("u5") + 1) + " (0基=" + rankOf("u5") + ")");
        sb.add("rankAround(u5, 1): " + rankAround("u5", 1));
        return sb.toString() + "\n";
    }
}
