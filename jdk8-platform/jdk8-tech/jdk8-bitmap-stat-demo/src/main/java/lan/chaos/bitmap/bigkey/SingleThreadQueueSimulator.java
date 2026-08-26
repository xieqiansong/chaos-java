package lan.chaos.bitmap.bigkey;

/**
 * Redis 单线程事件循环模拟：命令排队串行执行，每个命令的完成时间 = 前方所有命令耗时累计。
 *
 * <p>演示大 key 位运算的真实危害：BITCOUNT/BITOP 是 O(N) 的 CPU 密集命令，在 Redis 单线程
 * 主循环里会把后续所有命令（GET/SETBIT）整体推迟——不是「这条命令慢」，是
 * 「整条链路的尾延迟被拉长」。</p>
 */
public final class SingleThreadQueueSimulator {

    private SingleThreadQueueSimulator() {
    }

    /** 模拟一次 BITCOUNT 的耗时成本（线性于位数，每 long 1 时间单位）。 */
    public static long bitCountCost(long bits) {
        return (bits + 63) >>> 6;
    }

    /** 给定命令耗时序列，返回每个命令的完成时间（累计和）。 */
    public static long[] finishTimes(long[] cmdCosts) {
        long[] finish = new long[cmdCosts.length];
        long acc = 0;
        for (int i = 0; i < cmdCosts.length; i++) {
            acc += cmdCosts[i];
            finish[i] = acc;
        }
        return finish;
    }

    /** 指定下标区间内命令的最大完成时间，用于衡量尾延迟。 */
    public static long maxFinishOf(long[] finishTimes, int fromInclusive, int toExclusive) {
        long max = 0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            max = Math.max(max, finishTimes[i]);
        }
        return max;
    }

    /** 把单个大命令拆成 shards 片，返回每片耗时（均匀分摊）。 */
    public static long[] splitCost(long bigCost, int shards) {
        if (shards <= 0) {
            throw new IllegalArgumentException("shards must be > 0");
        }
        long[] costs = new long[shards];
        long base = bigCost / shards;
        long rem = bigCost % shards;
        for (int i = 0; i < shards; i++) {
            costs[i] = base + (i < rem ? 1 : 0);
        }
        return costs;
    }
}
