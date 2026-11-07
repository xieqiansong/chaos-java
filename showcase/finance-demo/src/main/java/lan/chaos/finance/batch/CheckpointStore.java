package lan.chaos.finance.batch;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批处理断点（Checkpoint）。
 *
 * <p>WHY：日终批处理数据量大，中途宕机不能从头重跑（会重复记账 = 资损），
 * 也不能跳过没跑完的（会漏账）。用「已完成工作项 ID 集合」做 checkpoint，
 * 重跑时只补未完成的 —— 这就是批处理的「不重不丢、断点续跑」。
 */
public class CheckpointStore {
    private final Set<String> done = ConcurrentHashMap.newKeySet();

    /** 幂等标记：已完成返回 true；未完成则标记并返回 false。 */
    public boolean markIfAbsent(String itemId) {
        return !done.add(itemId);
    }

    public boolean isDone(String itemId) {
        return done.contains(itemId);
    }

    public int doneCount() {
        return done.size();
    }
}
