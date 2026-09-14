package lan.chaos.leaderboard;

import lan.chaos.leaderboard.common.model.RankEntry;
import lan.chaos.leaderboard.core.ZsetLeaderboardService;
import lan.chaos.leaderboard.doublecache.TopBoardCacheService;
import lan.chaos.leaderboard.hot.HotDataLeaderboardService;
import lan.chaos.leaderboard.pipeline.AsyncScorePipeline;
import lan.chaos.leaderboard.shard.ShardedLeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景集成测试（默认 memory 存储，纯内存零外部依赖，任何环境直接过）。
 *
 * <p>上层五个能力包只依赖 {@link lan.chaos.leaderboard.common.store.ScoreBoard} 抽象，
 * 这里用内存实现完整验证各场景语义，无需 Redis。</p>
 */
@SpringBootTest
class LeaderboardScenarioTest {

    @Autowired
    private ZsetLeaderboardService core;
    @Autowired
    private TopBoardCacheService doubleCache;
    @Autowired
    private HotDataLeaderboardService hot;
    @Autowired
    private ShardedLeaderboardService shard;
    @Autowired
    private AsyncScorePipeline pipeline;

    @Test
    void core_topN_rank_around() {
        // updateScore 累加；topN 高分在前；rankOf 0 基；rankAround 取附近
        core.setScore("u1", 100);
        core.updateScore("u2", 300);
        core.updateScore("u3", 200);
        List<RankEntry> top = core.topN(3);
        assertEquals("u2", top.get(0).getMember());   // 300 第一
        assertEquals("u3", top.get(1).getMember());   // 200 第二
        assertEquals("u1", top.get(2).getMember());   // 100 第三
        assertEquals(0L, core.rankOf("u2"));           // u2 第一名
        assertEquals(1L, core.rankOf("u3"));

        // u2=300, u3=200, u1=100 -> u3 附近应包含 u2(前) 与 u1(后)
        List<RankEntry> around = core.rankAround("u3", 1);
        assertEquals(3, around.size());
        assertEquals("u2", around.get(0).getMember());
        assertEquals("u3", around.get(1).getMember());
        assertEquals("u1", around.get(2).getMember());
    }

    @Test
    void doubleCache_staleUntilRefresh() {
        for (int i = 1; i <= 200; i++) {
            doubleCache.updateScore("u" + i, i);
        }
        List<RankEntry> first = doubleCache.getTop();
        String topMemberBefore = first.get(0).getMember();

        // 把最低分 u1 刷成最高，未刷新时缓存仍返回旧 Top
        doubleCache.updateScore("u1", 1_000_000);
        List<RankEntry> cached = doubleCache.getTop();
        assertEquals(topMemberBefore, cached.get(0).getMember());

        // 主动刷新后立即反映最新
        doubleCache.refresh();
        List<RankEntry> fresh = doubleCache.getTop();
        assertEquals("u1", fresh.get(0).getMember());
        assertTrue(doubleCache.hitRate() > 0, "重复读取应命中本地缓存");
    }

    @Test
    void hot_dualWriteAndTrim() {
        for (int i = 1; i <= 1500; i++) {
            hot.updateScore("u" + i, i);
        }
        // 双写：热数据板与全量板分数一致
        assertEquals(1500, hot.fullSize());
        assertTrue(hot.hotSize() >= 1500); // 未裁剪前热板也含全部
        // 裁剪后收敛到 Top1000
        hot.trimHot();
        assertEquals(1000, hot.hotSize());
        // 热板 Top5 应等于全局最高 5 名
        assertEquals("u1500", hot.topFromHot(5).get(0).getMember());
    }

    @Test
    void shard_globalTopK_and_rank() {
        for (int i = 1; i <= 1000; i++) {
            shard.updateScore("u" + i, i);
        }
        List<RankEntry> top = shard.globalTopK(5);
        assertEquals(5, top.size());
        assertEquals("u1000", top.get(0).getMember());  // 最高分第一
        assertEquals(0L, shard.globalRank("u1000"));      // 全局第一
        // 校验全局 Top K 与逐分片合并排序一致（globalTopK 内部已合并）
        for (int i = 1; i < top.size(); i++) {
            assertTrue(top.get(i - 1).getScore() >= top.get(i).getScore());
        }
    }

    @Test
    void pipeline_asyncFlush_reflectsSum() throws Exception {
        for (int i = 0; i < 1000; i++) {
            pipeline.submit("u1", 1);   // u1 累计 +1000
            pipeline.submit("u2", 2);   // u2 累计 +2000
        }
        assertTrue(pipeline.awaitQuiescent(5000));
        assertEquals(1000.0, pipeline.topN(10).stream()
                .filter(e -> e.getMember().equals("u1")).findFirst().get().getScore(), 0.001);
        assertTrue(pipeline.stats().contains("flushed=2000"));
    }
}
