package lan.chaos.bitmap.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 场景 D：吞吐对比（BitMap vs HashSet）。
 * 同数据下千万级查询：GETBIT（连续内存 + 位运算）vs Set.contains（对象 hash 定位）；
 * 统计：BITCOUNT（O(N/64) 全量）vs Set.size()（O(1)）。
 * 演示 Bitmap 的内存连续性与 cache 友好性。
 */
public final class BitMapBenchmark {

    private final long devices;
    private final long online;
    private final int queries;

    public BitMapBenchmark(long devices, long online, int queries) {
        this.devices = devices;
        this.online = online;
        this.queries = queries;
    }

    public void run() {
        BitMap bitmap = new BitMap(devices);
        Set<Long> set = new HashSet<>();
        long step = devices / online; // 4：每第 4 台在线，两结构同一份数据
        for (long dev = 0; dev < devices; dev += step) {
            bitmap.setBit(dev);
            set.add(dev);
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long t0 = System.nanoTime();
        long bitHits = 0;
        for (int i = 0; i < queries; i++) {
            long dev = (long) (rnd.nextDouble() * devices);
            if (bitmap.getBit(dev)) {
                bitHits++;
            }
        }
        long getbitNanos = System.nanoTime() - t0;

        t0 = System.nanoTime();
        long setHits = 0;
        for (int i = 0; i < queries; i++) {
            long dev = (long) (rnd.nextDouble() * devices);
            if (set.contains(dev)) {
                setHits++;
            }
        }
        long containsNanos = System.nanoTime() - t0;

        t0 = System.nanoTime();
        long bitmapCount = bitmap.bitCount();
        long bitcountNanos = System.nanoTime() - t0;
        long setCount = set.size();

        System.out.println("== 场景 D：吞吐对比（BitMap vs HashSet，同一份在线数据） ==");
        System.out.printf("  数据规模      : %,d 设备、%,d 在线、位图内存 %.2f MB%n",
                devices, online, bitmap.memoryBytes() / 1024.0 / 1024.0);
        System.out.printf("  查询 %,d 次   : GETBIT %d 命中 %.2f ms | contains %d 命中 %.2f ms%n",
                queries, bitHits, getbitNanos / 1e6, setHits, containsNanos / 1e6);
        System.out.printf("  统计          : BITCOUNT = %,d 耗时 %.2f ms | Set.size() = %,d（O(1)）%n",
                bitmapCount, bitcountNanos / 1e6, setCount);
        System.out.println();
    }
}
