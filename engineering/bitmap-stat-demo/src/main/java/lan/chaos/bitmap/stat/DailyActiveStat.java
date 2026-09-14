package lan.chaos.bitmap.stat;

import lan.chaos.bitmap.core.BitMap;

/**
 * 场景 B：日活统计（按天位图 + BITOP）。
 * 按天一个位图、SETBIT 标记当日活跃设备；BITOP OR 得近 N 天活跃（滚动 DAU），
 * BITOP AND 得连续 N 天活跃（核心用户）。按天分 key 本身就是时间维度分片。
 */
public final class DailyActiveStat {

    private static final long DEVICES = 12_000_000L;

    public void run() {
        System.out.println("== 场景 B：日活统计（按天位图 + BITOP） ==");
        int days = 7;
        BitMap[] dayMaps = new BitMap[days];
        for (int d = 0; d < days; d++) {
            BitMap b = new BitMap(DEVICES);
            int step = 2 + d; // 每天活跃密度不同，保证天与天之间部分重叠
            for (long dev = d % step; dev < DEVICES; dev += step) {
                b.setBit(dev);
            }
            dayMaps[d] = b;
        }
        for (int d = 0; d < days; d++) {
            System.out.printf("  day%d 活跃           : %,d%n", d, dayMaps[d].bitCount());
        }

        long t0 = System.nanoTime();
        long union = unionActiveCount(dayMaps);
        long unionNanos = System.nanoTime() - t0;
        t0 = System.nanoTime();
        long inter = intersectActiveCount(dayMaps);
        long interNanos = System.nanoTime() - t0;

        System.out.printf("  近 7 天活跃(BITOP OR)  : %,d, 耗时 %.2f ms%n", union, unionNanos / 1e6);
        System.out.printf("  连续 7 天活跃(BITOP AND): %,d, 耗时 %.2f ms%n", inter, interNanos / 1e6);
        System.out.println();
    }

    /** 近 N 天活跃 = 逐天 BITOP OR 后的置位数。 */
    public static long unionActiveCount(BitMap[] days) {
        if (days == null || days.length == 0) {
            return 0;
        }
        BitMap acc = days[0];
        for (int i = 1; i < days.length; i++) {
            acc = acc.bitOpOr(days[i]);
        }
        return acc.bitCount();
    }

    /** 连续 N 天活跃 = 逐天 BITOP AND 后的置位数。 */
    public static long intersectActiveCount(BitMap[] days) {
        if (days == null || days.length == 0) {
            return 0;
        }
        BitMap acc = days[0];
        for (int i = 1; i < days.length; i++) {
            acc = acc.bitOpAnd(days[i]);
        }
        return acc.bitCount();
    }
}
