package lan.chaos.scheduler.common.model;

/**
 * 定时任务演示用的样例「业务数据」。
 *
 * <p>为什么需要它：调度框架只负责「什么时候触发」，被触发的业务逻辑需要一个具体对象来
 * 体现「输入 → 输出」。这里用最小字段模拟一次要跑的批处理任务（如「清理 N 天前的日志」）。
 */
public class JobSample {

    /** 任务名（如 cleanup-expired-logs）。 */
    private final String name;
    /** 触发时刻的 epoch 毫秒，用来在日志里呈现「输入」。 */
    private final long triggeredAt;

    public JobSample(String name, long triggeredAt) {
        this.name = name;
        this.triggeredAt = triggeredAt;
    }

    public String name() { return name; }

    public long triggeredAt() { return triggeredAt; }

    /** 默认样例工厂：避免调用方自己拼装数据。 */
    public static JobSample sampleJob(String name) {
        return new JobSample(name, System.currentTimeMillis());
    }

    /** 业务处理结果：把触发时刻格式化输出，作为「输出」可被断言/打印。 */
    public String describe() {
        return String.format("[job=%s] triggeredAt=%d -> 已执行", name, triggeredAt);
    }
}
