package lan.chaos.leaderboard.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排行榜条目：成员（用户 ID）+ 积分 + 名次。
 *
 * <p>{@code rank} 为 0 基（0 = 第一名），与 Redis {@code ZREVRANK} 一致。
 * 同分时按 member 字典序做确定性排序（保证 Top K 结果可复现）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankEntry {

    /** 成员标识，通常为 userId 字符串 */
    private String member;

    /** 积分（对应 ZSET score，double） */
    private double score;

    /** 名次，0 基；0 表示第一名 */
    private long rank;

    @Override
    public String toString() {
        return String.format("#%d %s=%.0f", rank + 1, member, score);
    }
}
