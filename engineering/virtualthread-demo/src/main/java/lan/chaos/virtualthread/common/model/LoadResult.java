package lan.chaos.virtualthread.common.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 一组并发执行的结果度量：
 * costMillis        —— 全部任务完成的总耗时（墙钟时间）。
 * throughputPerSec  —— 每秒完成任务数（taskCount / costSeconds）。
 * peakConcurrency   —— 同时处于运行态的线程数峰值（阻塞是否让出调度器的证据）。
 */
@Getter
@Builder
public class LoadResult {

    private final String label;
    private final int taskCount;
    private final long ioMillis;
    private final long costMillis;
    private final int peakConcurrency;

    public long throughputPerSec() {
        return costMillis == 0 ? Long.MAX_VALUE : taskCount * 1000L / costMillis;
    }

    public String pretty() {
        return String.format(
                "  [%s] 任务数=%d 单任务IO=%dms 总耗时=%dms 吞吐=%d任务/s 峰值并发=%d",
                label, taskCount, ioMillis, costMillis, throughputPerSec(), peakConcurrency);
    }
}
