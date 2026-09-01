package lan.chaos.virtualthread.common.util;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 延迟采样器：预分配 long 数组、无锁写入，跑完后一次性排序算分位。
 * WHY：压测中每任务记一次延迟，若用同步集合或装箱 Long 会引入可观开销、污染被测代码；
 * 定长数组 + 原子下标既无锁也无装箱，采样成本可忽略。
 * 记录的是「提交 → 完成」的端到端耗时，因此天然包含排队等待——这正是线程池饱和要暴露的代价。
 */
public final class LatencyRecorder {

    private final long[] samples;
    private final AtomicInteger index = new AtomicInteger();

    public LatencyRecorder(int capacity) {
        this.samples = new long[Math.max(capacity, 1)];
    }

    /** 记录一次采样，单位纳秒；超出容量时丢弃（不应发生，容量按任务数预分配）。 */
    public void record(long nanos) {
        int i = index.getAndIncrement();
        if (i < samples.length) {
            samples[i] = nanos;
        }
    }

    public int count() {
        return Math.min(index.get(), samples.length);
    }

    /** 升序快照，仅在统计阶段调用一次。 */
    private long[] sorted() {
        long[] copy = Arrays.copyOf(samples, count());
        Arrays.sort(copy);
        return copy;
    }

    /** 一次排序算出全部统计量：分开了算会导致每个指标各排一次序。 */
    public Stats stats() {
        long[] s = sorted();
        if (s.length == 0) {
            return new Stats(0D, 0D, 0D, 0D, 0L);
        }
        long sum = 0;
        for (long v : s) {
            sum += v;
        }
        return new Stats(sum / (double) s.length / 1_000_000D,
                pick(s, 0.50), pick(s, 0.95), pick(s, 0.99),
                s[s.length - 1] / 1_000_000L);
    }

    /** 最近秩(nearest-rank)分位数，返回毫秒，纳秒存储、毫秒呈现。 */
    private static double pick(long[] sorted, double ratio) {
        int rank = (int) Math.ceil(ratio * sorted.length) - 1;
        rank = Math.max(0, Math.min(rank, sorted.length - 1));
        return sorted[rank] / 1_000_000D;
    }

    /** 单轮采样的统计结果。 */
    public record Stats(double avgMillis, double p50Millis, double p95Millis,
                        double p99Millis, long maxMillis) {
    }

    public double percentile(double ratio) {
        long[] s = sorted();
        return s.length == 0 ? 0D : pick(s, ratio);
    }

    public double p50() {
        return stats().p50Millis();
    }

    public double p95() {
        return stats().p95Millis();
    }

    public double p99() {
        return stats().p99Millis();
    }

    public double avgMillis() {
        return stats().avgMillis();
    }

    public long maxMillis() {
        return stats().maxMillis();
    }
}
