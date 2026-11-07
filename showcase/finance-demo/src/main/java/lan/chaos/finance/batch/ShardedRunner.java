package lan.chaos.finance.batch;

import java.util.List;
import java.util.function.Consumer;

/**
 * 分片 + 断点续跑执行器（P4，对应对话「时间与批量」）。
 *
 * <p>WHY：金融系统联机交易（OLTP）与批量处理（Batch）并行。日终批处理要在
 * 特定时间窗口内跑完，且必须满足四件事：
 * <ul>
 *   <li><b>分片</b>：大数据按 {@code shard} 分组，便于并行/限流；</li>
 *   <li><b>不重</b>：每个工作项靠 {@link CheckpointStore#markIfAbsent} 幂等，重复跑不重复处理；</li>
 *   <li><b>不丢</b>：未成功的工作项下一轮继续；</li>
 *   <li><b>断点续跑</b>：模拟中途失败后重跑，只补失败的，已完成的不重跑。</li>
 * </ul>
 *
 * <p>防御性思维③：消息/任务重复消费会怎样？→ checkpoint 幂等标记兜底。
 */
public class ShardedRunner {

    /** 批处理结果（可观察、可断言）。 */
    public static class Result {
        private final int processed;
        private final int skippedDuplicated;
        private final int shards;

        Result(int processed, int skippedDuplicated, int shards) {
            this.processed = processed;
            this.skippedDuplicated = skippedDuplicated;
            this.shards = shards;
        }

        public int getProcessed() {
            return processed;
        }

        public int getSkippedDuplicated() {
            return skippedDuplicated;
        }

        public int getShards() {
            return shards;
        }

        @Override
        public String toString() {
            return "Result{processed=" + processed + ", skipped=" + skippedDuplicated
                    + ", shards=" + shards + '}';
        }
    }

    /**
     * 执行一轮批处理。
     *
     * @param items     工作项（每个带 shard 号）
     * @param handler   单条处理逻辑；抛异常视为该条失败，可被下一轮重试
     * @param failOnId  模拟中途失败的项 ID（null 表示不注入失败）；失败项不标记完成
     */
    public Result run(List<WorkItem> items, CheckpointStore checkpoint,
                      Consumer<WorkItem> handler, String failOnId) {
        int processed = 0;
        int skipped = 0;
        int maxShard = -1;
        for (WorkItem item : items) {
            maxShard = Math.max(maxShard, item.getShard());
        }
        for (WorkItem item : items) {
            if (checkpoint.isDone(item.getId())) {
                skipped++; // 已完成，断点续跑时跳过 → 不重
                continue;
            }
            if (item.getId().equals(failOnId)) {
                // 模拟宕机：此时尚未标记完成 → 下一轮会重试该条 → 不丢
                throw new IllegalStateException("批处理中途失败于 " + item.getId());
            }
            handler.accept(item);
            checkpoint.markIfAbsent(item.getId()); // 成功后才标记完成
            processed++;
        }
        return new Result(processed, skipped, maxShard + 1);
    }
}
