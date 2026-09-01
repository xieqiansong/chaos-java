package lan.chaos.virtualthread.common.model;

/**
 * 一轮压测的结果度量（比机制演示层的 {@link LoadResult} 更全，增加延迟分位与失败/拒绝计数）。
 *
 * <ul>
 *   <li>throughput —— 成功任务数 / 墙钟耗时：线程池饱和时直接塌陷，是最直观的收益指标。</li>
 *   <li>p99        —— 端到端延迟分位（含排队）：平台池排队时先崩的是尾延迟，往往早于吞吐塌陷。</li>
 *   <li>rejected   —— 被饱和策略拒绝的任务数：有界队列 + AbortPolicy 下的直接损失。</li>
 *   <li>peakConcurrency —— 真正同时执行的任务数峰值：平台池恒等于线程数，虚拟线程可远超。</li>
 * </ul>
 */
public class BenchResult {

    private final String label;
    private final int taskCount;
    private final int success;
    private final int failed;
    private final int rejected;
    private final long costMillis;
    private final int peakConcurrency;
    private final double avgMillis;
    private final double p50Millis;
    private final double p99Millis;
    private final long maxMillis;

    public BenchResult(String label, int taskCount, int success, int failed, int rejected,
                       long costMillis, int peakConcurrency,
                       double avgMillis, double p50Millis, double p99Millis, long maxMillis) {
        this.label = label;
        this.taskCount = taskCount;
        this.success = success;
        this.failed = failed;
        this.rejected = rejected;
        this.costMillis = costMillis;
        this.peakConcurrency = peakConcurrency;
        this.avgMillis = avgMillis;
        this.p50Millis = p50Millis;
        this.p99Millis = p99Millis;
        this.maxMillis = maxMillis;
    }

    /** 每秒完成任务数（按成功数计，避免把失败/拒绝算进吞吐）。 */
    public double throughputPerSec() {
        return costMillis == 0 ? 0D : success * 1000D / costMillis;
    }

    public String label() {
        return label;
    }

    public int taskCount() {
        return taskCount;
    }

    public int success() {
        return success;
    }

    public int failed() {
        return failed;
    }

    public int rejected() {
        return rejected;
    }

    public long costMillis() {
        return costMillis;
    }

    public int peakConcurrency() {
        return peakConcurrency;
    }

    public double avgMillis() {
        return avgMillis;
    }

    public double p50Millis() {
        return p50Millis;
    }

    public double p99Millis() {
        return p99Millis;
    }

    public long maxMillis() {
        return maxMillis;
    }

    /** 控制台单行输出。 */
    public String pretty() {
        return String.format(
                "  [%s] 吞吐=%,.0f 任务/s  耗时=%dms  延迟 avg=%.1fms p50=%.1fms p99=%.1fms max=%dms  "
                        + "成功=%d 失败=%d 拒绝=%d 峰值并发=%d",
                label, throughputPerSec(), costMillis, avgMillis, p50Millis, p99Millis, maxMillis,
                success, failed, rejected, peakConcurrency);
    }

    /** markdown 表格行。 */
    public String tableRow() {
        return String.format("| %s | %,.0f | %d | %.1f | %.1f | %.1f | %d | %d | %d | %d |",
                label, throughputPerSec(), costMillis, avgMillis, p50Millis, p99Millis, maxMillis,
                success, failed, rejected);
    }
}
