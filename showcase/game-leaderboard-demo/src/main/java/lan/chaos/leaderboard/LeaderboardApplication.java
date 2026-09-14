package lan.chaos.leaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 游戏实时积分排行榜 Demo 启动类。
 *
 * <p>核心思路（对应牛客面经「百万用户实时积分排行榜」方案）：
 * 以 Redis ZSET 为内核，叠加<b>双层缓存 / 热数据双写 / 分片聚合 / 异步更新流水线</b>四层优化。</p>
 *
 * <p>默认 {@code leaderboard.store=memory}（纯内存零外部依赖），直接 {@code mvn test} 或跑 {@link DemoApp} 即可看效果；
 * 配置 {@code --leaderboard.store=redis} 后切换到真实 Redis ZSET（需本地 Redis，见 docker-compose.yml）。</p>
 */
@SpringBootApplication
public class LeaderboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaderboardApplication.class, args);
    }
}
