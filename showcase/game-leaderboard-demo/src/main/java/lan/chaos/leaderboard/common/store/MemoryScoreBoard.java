package lan.chaos.leaderboard.common.store;

import lan.chaos.leaderboard.common.model.RankEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 纯内存 ScoreBoard 实现（零外部依赖）。
 *
 * <p>用 {@link ConcurrentHashMap} 存 member→score，读时排序得到有序视图。
 * 排序复杂度 O(N log N)，<b>仅用于单元测试与演示</b>；百万级真实场景请走 {@link RedisScoreBoard}。
 * 同分按 member 字典序小者靠前。</p>
 */
public class MemoryScoreBoard implements ScoreBoard {

    private final String name;
    private final Map<String, Double> scores = new ConcurrentHashMap<>();

    public MemoryScoreBoard() {
        this("memory");
    }

    public MemoryScoreBoard(String name) {
        this.name = name;
    }

    /** 全量有序视图：score 倒序，同分 member 升序 */
    private List<RankEntry> sortedAll() {
        List<RankEntry> list = new ArrayList<>(scores.size());
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            list.add(new RankEntry(e.getKey(), e.getValue(), 0));
        }
        list.sort(Comparator.<RankEntry>comparingDouble(RankEntry::getScore)
                .reversed()
                .thenComparing(RankEntry::getMember));
        return list;
    }

    @Override
    public void setScore(String member, double score) {
        scores.put(member, score);
    }

    @Override
    public double incrementScore(String member, double delta) {
        return scores.merge(member, delta, Double::sum);
    }

    @Override
    public Double getScore(String member) {
        return scores.get(member);
    }

    @Override
    public List<RankEntry> topDesc(long start, long end) {
        List<RankEntry> all = sortedAll();
        long size = all.size();
        if (start >= size) {
            return new ArrayList<>();
        }
        long to = Math.min(end, size - 1);
        List<RankEntry> result = new ArrayList<>();
        for (long i = start; i <= to; i++) {
            RankEntry e = all.get((int) i);
            result.add(new RankEntry(e.getMember(), e.getScore(), i));
        }
        return result;
    }

    @Override
    public Long rankOf(String member) {
        List<RankEntry> all = sortedAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getMember().equals(member)) {
                return (long) i;
            }
        }
        return null;
    }

    @Override
    public long size() {
        return scores.size();
    }

    @Override
    public void remove(String member) {
        scores.remove(member);
    }

    @Override
    public void clear() {
        scores.clear();
    }

    @Override
    public String toString() {
        return "MemoryScoreBoard(" + name + ", size=" + scores.size() + ")";
    }
}
