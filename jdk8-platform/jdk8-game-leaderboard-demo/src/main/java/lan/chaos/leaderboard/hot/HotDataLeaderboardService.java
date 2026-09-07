package lan.chaos.leaderboard.hot;

import lan.chaos.leaderboard.common.model.RankEntry;
import lan.chaos.leaderboard.common.store.ScoreBoard;
import lan.chaos.leaderboard.common.store.ScoreBoardFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

/**
 * ③ 热数据双写（对应面经「热数据特殊处理」）。
 *
 * <p>同时维护两套 ZSET：
 * <ul>
 *     <li><b>全量板 fullBoard</b>：所有用户；</li>
 *     <li><b>热数据板 hotBoard</b>：仅保留 Top {@code hotCapacity}（如 Top1000）的专用小 ZSET。</li>
 * </ul>
 * 更新时<b>双写</b>（全量 + 热数据），查询 Top K 只走更小的 hotBoard，进一步降延迟。
 * 周期性 {@link #trimHot()} 把 hotBoard 裁剪回 Top N，避免其无限膨胀。</p>
 */
@Service
public class HotDataLeaderboardService {

    private final ScoreBoard fullBoard;
    private final ScoreBoard hotBoard;
    private final int hotCapacity;

    @Autowired
    public HotDataLeaderboardService(ScoreBoardFactory factory,
                                      @Value("${leaderboard.hot-capacity:1000}") int hotCapacity) {
        this.fullBoard = factory.create("hot-full");
        this.hotBoard = factory.create("hot-top");
        this.hotCapacity = hotCapacity;
    }

    /** 双写：全量板 + 热数据板都更新 */
    public void updateScore(String userId, double delta) {
        fullBoard.incrementScore(userId, delta);
        hotBoard.incrementScore(userId, delta);
    }

    /** 查询 Top K：只走热数据板（更小、更快） */
    public List<RankEntry> topFromHot(int k) {
        return hotBoard.topN(k);
    }

    /**
     * 把热数据板裁剪到 Top {@code hotCapacity}。
     * 实现：取全量板当前 Top N，重建热数据板（清空后只回写这 N 名）。
     */
    public void trimHot() {
        List<RankEntry> keep = fullBoard.topDesc(0, hotCapacity - 1);
        hotBoard.clear();
        for (RankEntry e : keep) {
            hotBoard.setScore(e.getMember(), e.getScore());
        }
    }

    public long hotSize() {
        return hotBoard.size();
    }

    public long fullSize() {
        return fullBoard.size();
    }

    public String run() {
        // 1500 个用户（超过 hotCapacity=1000），分数 1..1500
        for (int i = 1; i <= 1500; i++) {
            updateScore("u" + i, i);
        }
        StringJoiner sb = new StringJoiner("\n");
        sb.add("双写后: fullSize=" + fullSize() + ", hotSize(裁剪前)=" + hotSize());
        trimHot();
        sb.add("trimHot() 后: hotSize=" + hotSize() + "（已收敛到 Top" + hotCapacity + "）");
        sb.add("topFromHot(5): " + topFromHot(5));
        return sb.toString() + "\n";
    }
}
