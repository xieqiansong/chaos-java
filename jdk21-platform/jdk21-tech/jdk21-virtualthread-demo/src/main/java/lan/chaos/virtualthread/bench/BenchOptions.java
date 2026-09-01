package lan.chaos.virtualthread.bench;

import lombok.Builder;
import lombok.Getter;

/**
 * 一轮压测的参数。所有场景共用同一套口径，保证结果横向可比。
 *
 * <ul>
 *   <li>taskCount    —— 本轮提交的总任务数。</li>
 *   <li>concurrency  —— 在途任务上限（闭环负载）：提交前取许可、任务完成后归还，
 *                      因此「并发 2000」意味着最多 2000 个请求同时在途，而非瞬间全灌进去。</li>
 *   <li>platformThreads —— 平台线程池的固定线程数，是本组对照实验的核心自变量。</li>
 *   <li>queueCapacity —— 平台池的有界队列容量；队列满后按 AbortPolicy 拒绝，用于复现「排队 → 拒绝」。</li>
 *   <li>warmupRounds —— 预热轮数，抵消 JIT 与线程池创建的首次开销，避免首轮结果虚低。</li>
 *   <li>ioMillis / jitterMillis —— 单任务模拟的等待时长与抖动。</li>
 * </ul>
 */
@Getter
@Builder
public class BenchOptions {

    @Builder.Default
    private int taskCount = 5000;

    @Builder.Default
    private int concurrency = 2000;

    @Builder.Default
    private int platformThreads = 200;

    @Builder.Default
    private int queueCapacity = 100_000;

    @Builder.Default
    private int warmupRounds = 1;

    @Builder.Default
    private long ioMillis = 20;

    @Builder.Default
    private long jitterMillis = 0;

    /** 控制台/报告用的参数摘要。 */
    public String describe() {
        return String.format("任务数=%d 并发=%d 平台池线程=%d 队列=%d 单任务IO=%dms 抖动=%dms",
                taskCount, concurrency, platformThreads, queueCapacity, ioMillis, jitterMillis);
    }

    /** 预热轮的任务数：取十分之一，够触发 JIT 又不显著拉长总时长。 */
    public int warmupTaskCount() {
        return Math.max(100, taskCount / 10);
    }
}
