package lan.chaos.leaderboard;

import lan.chaos.leaderboard.core.ZsetLeaderboardService;
import lan.chaos.leaderboard.doublecache.TopBoardCacheService;
import lan.chaos.leaderboard.hot.HotDataLeaderboardService;
import lan.chaos.leaderboard.pipeline.AsyncScorePipeline;
import lan.chaos.leaderboard.shard.ShardedLeaderboardService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 控制台 Runner：分节打印每个能力的「输入→输出」，
 * 想纯看效果不写测试时，直接跑这个 main（默认 memory 存储，零外部依赖）。
 */
public class DemoApp {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext ctx =
                     SpringApplication.run(LeaderboardApplication.class, args)) {
            section("1. 核心 ZSET 排行榜 core", ctx.getBean(ZsetLeaderboardService.class).run());
            section("2. 双层缓存 Top100 doublecache", ctx.getBean(TopBoardCacheService.class).run());
            section("3. 热数据双写 hot", ctx.getBean(HotDataLeaderboardService.class).run());
            section("4. 分片聚合 shard", ctx.getBean(ShardedLeaderboardService.class).run());
            section("5. 异步更新流水线 pipeline", ctx.getBean(AsyncScorePipeline.class).run());
        }
    }

    private static void section(String title, String body) {
        System.out.println("\n========== " + title + " ==========");
        System.out.print(body);
    }
}
