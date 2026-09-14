package lan.chaos.leaderboard.shard;

import lan.chaos.leaderboard.common.model.RankEntry;
import lan.chaos.leaderboard.common.store.ScoreBoard;
import lan.chaos.leaderboard.common.store.ScoreBoardFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ④ 分片 + 跨分片聚合（对应面经「集群化扩展（数据分片）」与「跨分片 Top K 聚合」）。
 *
 * <p>按 {@code hash(userId) % shardCount} 把用户打散到多个 ZSET 分片，单分片容量下降、写入可水平扩展。
 * 查询全局 Top K 时，从每个分片取本地 Top K，再合并归并出全局 Top K
 * （对应文档「每个分片本地 Top1000 → 定时聚合 → 全局 Top100」的思路）。</p>
 */
@Service
public class ShardedLeaderboardService {

    private final List<ScoreBoard> shards;
    private final int shardCount;

    @Autowired
    public ShardedLeaderboardService(ScoreBoardFactory factory,
                                      @Value("${leaderboard.shard-count:4}") int shardCount) {
        this.shardCount = shardCount;
        this.shards = new ArrayList<>(shardCount);
        for (int i = 0; i < shardCount; i++) {
            shards.add(factory.create("shard-" + i));
        }
    }

    /** 分片路由：稳定的哈希取模 */
    public int shardIndex(String userId) {
        return Math.floorMod(userId.hashCode(), shardCount);
    }

    /** 更新积分：路由到对应分片 */
    public double updateScore(String userId, double delta) {
        return shards.get(shardIndex(userId)).incrementScore(userId, delta);
    }

    /** 某用户全局名次（0 基）：统计所有分片中排名高于它的成员数 */
    public Long globalRank(String userId) {
        Double score = scoreOf(userId);
        if (score == null) {
            return null;
        }
        long above = 0;
        for (ScoreBoard shard : shards) {
            for (RankEntry e : shard.topDesc(0, shard.size() - 1)) {
                if (e.getScore() > score
                        || (e.getScore() == score && e.getMember().compareTo(userId) < 0)) {
                    above++;
                }
            }
        }
        return above;
    }

    private Double scoreOf(String userId) {
        return shards.get(shardIndex(userId)).getScore(userId);
    }

    /** 全局 Top K：各分片取本地 Top K 后合并归并 */
    public List<RankEntry> globalTopK(int k) {
        List<RankEntry> merged = new ArrayList<>();
        for (ScoreBoard shard : shards) {
            merged.addAll(shard.topN(k));
        }
        merged.sort(Comparator.<RankEntry>comparingDouble(RankEntry::getScore)
                .reversed()
                .thenComparing(RankEntry::getMember));
        List<RankEntry> result = new ArrayList<>();
        for (int i = 0; i < Math.min(k, merged.size()); i++) {
            result.add(new RankEntry(merged.get(i).getMember(), merged.get(i).getScore(), i));
        }
        return result;
    }

    public String run() {
        // 1000 个用户，分数 1..1000，按分片打散
        for (int i = 1; i <= 1000; i++) {
            updateScore("u" + i, i);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("分片数=").append(shardCount)
                .append("，各分片容量=");
        for (int i = 0; i < shardCount; i++) {
            sb.append(shards.get(i).size());
            if (i < shardCount - 1) {
                sb.append("/");
            }
        }
        sb.append("\n");
        sb.append("globalTopK(5): ").append(globalTopK(5)).append("\n");
        sb.append("globalRank(u1000) -> #").append(globalRank("u1000") + 1)
                .append(" (应为 #1)").append("\n");
        return sb.toString();
    }
}
