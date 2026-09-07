package lan.chaos.leaderboard.common.store;

import lan.chaos.leaderboard.common.model.RankEntry;

import java.util.List;

/**
 * 有序积分榜存储抽象（★ 架构核心）。
 *
 * <p>把「底层存储」从业务逻辑中解耦：
 * <ul>
 *     <li>{@link MemoryScoreBoard}：纯内存实现，用于单元测试与零依赖演示（{@code store=memory}）。</li>
 *     <li>{@link RedisScoreBoard}：基于 Redis ZSET（跳跃表 + 哈希表），生产真实后端（{@code store=redis}）。</li>
 * </ul>
 * 上层五个能力包（core / doublecache / hot / shard / pipeline）只依赖此接口，
 * 因此<b>全部逻辑都能在内存下被完整测试</b>，无需起 Redis。</p>
 *
 * <p>语义约定（与 Redis ZSET 对齐）：
 * <ul>
 *     <li>score 为 double；积分变化用 {@link #incrementScore} 原子累加。</li>
 *     <li>{@link #topDesc} 倒序（高分在前），rank 从 0 起。</li>
 *     <li>同分按 member 字典序小者靠前，保证 Top K 确定性。</li>
 * </ul>
 */
public interface ScoreBoard {

    /** 直接设置积分（覆盖） */
    void setScore(String member, double score);

    /** 原子增减积分，返回变更后的积分 */
    double incrementScore(String member, double delta);

    /** 取某成员积分；不在榜返回 null */
    Double getScore(String member);

    /**
     * 倒序 Top 区间（闭区间，0 基）。
     * {@code topDesc(0, 99)} 即 Top 100。
     */
    List<RankEntry> topDesc(long start, long end);

    /** 便捷方法：倒序 Top N（{@code topDesc(0, n-1)}） */
    default List<RankEntry> topN(int n) {
        return topDesc(0, n - 1);
    }

    /** 某成员名次（0 基）；不在榜返回 null */
    Long rankOf(String member);

    /** 榜上成员总数 */
    long size();

    /** 移除某成员 */
    void remove(String member);

    /** 清空（演示隔离用） */
    void clear();
}
