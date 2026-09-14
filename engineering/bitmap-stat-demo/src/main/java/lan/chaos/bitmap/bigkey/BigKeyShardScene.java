package lan.chaos.bitmap.bigkey;

import lan.chaos.bitmap.core.BitMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 场景 E：大 key 位运算瓶颈与拆分优化。
 *
 * <pre>
 *   E1 瓶颈复现：Redis 单线程下，大 BITCOUNT 把后续普通命令整体推迟（尾延迟恶化）
 *   E2 哈希拆分：单大 key 全量 BITCOUNT vs 8 片并行 BITCOUNT + 汇总
 *   E3 区域拆分：单区域统计命中单片（0 聚合）；全局统计跨片聚合
 *   E4 增量计数：SETBIT 时维护 count，在线数 O(1) 直读，完全绕开 BITCOUNT
 * </pre>
 */
public final class BigKeyShardScene {

    /** 大 key 规模：4 亿位 ≈ 50 MB（bitCountCost = 625 万单位）。 */
    private static final long BIG_KEY_BITS = 400_000_000L;
    private static final int SHARDS = 8;

    public void run() {
        System.out.println("== 场景 E：大 key 位运算瓶颈与拆分优化 ==");
        e1Bottleneck();
        e2HashShard();
        e3RegionShard();
        e4IncrementalCounter();
        System.out.println();
    }

    /** E1：Redis 单线程下大 BITCOUNT 阻塞后续命令（尾延迟恶化）。 */
    private void e1Bottleneck() {
        int n = 100; // 100 个普通命令，每命令 1 单位
        long bigCost = SingleThreadQueueSimulator.bitCountCost(BIG_KEY_BITS);

        long[] single = new long[n + 1];
        Arrays.fill(single, 1);
        single[n / 2] = bigCost; // 第 50 个命令是大 key BITCOUNT
        long[] finishSingle = SingleThreadQueueSimulator.finishTimes(single);
        long cmd51Single = finishSingle[n / 2 + 1]; // 第 51 个普通命令的完成时间

        long[] pieces = SingleThreadQueueSimulator.splitCost(bigCost, SHARDS);
        long[] split = new long[n + pieces.length];
        Arrays.fill(split, 1);
        System.arraycopy(pieces, 0, split, n / 2, pieces.length); // 大命令拆 8 片原位替换
        long[] finishSplit = SingleThreadQueueSimulator.finishTimes(split);
        long cmd51Split = finishSplit[n / 2 + 1];

        System.out.printf("  E1 单线程阻塞（%d 位 BITCOUNT 插在第 50 个普通命令前）:%n", BIG_KEY_BITS);
        System.out.printf("    单大 key : 第 51 个普通命令完成 = %,d 单位（被阻塞 %,d）%n",
                cmd51Single, cmd51Single - (n / 2 + 1));
        System.out.printf("    拆 %d 片  : 单片成本 = %,d → 第 51 个普通命令完成 = %,d（等待缩小至 1/%d）%n",
                SHARDS, pieces[0], cmd51Split, SHARDS);
        System.out.println();
    }

    /** E2：哈希拆分（crc32 均匀落片）+ 并行 BITCOUNT。 */
    private void e2HashShard() {
        BitMap big = new BitMap(BIG_KEY_BITS);
        for (long i = 0; i < BIG_KEY_BITS; i += 1000) {
            big.setBit(i);
        }
        long t0 = System.nanoTime();
        long countSingle = big.bitCount();
        long singleNanos = System.nanoTime() - t0;

        long perShard = BIG_KEY_BITS / SHARDS;
        HashSharder sharder = new HashSharder(SHARDS);
        BitMap[] shards = new BitMap[SHARDS];
        for (int s = 0; s < SHARDS; s++) {
            shards[s] = new BitMap(perShard);
        }
        for (long i = 0; i < BIG_KEY_BITS; i += 1000) {
            int s = sharder.shardOf("dev-" + i);
            shards[s].setBit(i / SHARDS);
        }

        ExecutorService pool = Executors.newFixedThreadPool(SHARDS);
        long countSharded;
        long shardNanos;
        try {
            List<Future<Long>> futures = new ArrayList<>();
            for (BitMap m : shards) {
                futures.add(pool.submit(m::bitCount));
            }
            t0 = System.nanoTime();
            countSharded = 0;
            for (Future<Long> f : futures) {
                countSharded += f.get();
            }
            shardNanos = System.nanoTime() - t0;
        } catch (Exception e) {
            throw new IllegalStateException("parallel bitcount failed", e);
        } finally {
            pool.shutdown();
        }

        System.out.printf("  E2 哈希拆分（%,d 位随机在线）:%n", BIG_KEY_BITS);
        System.out.printf("    单大 key BITCOUNT     : %,d, 耗时 %.2f ms%n", countSingle, singleNanos / 1e6);
        System.out.printf("    %d 片并行 + 汇总      : %,d, 耗时 %.2f ms（一致: %s）%n",
                SHARDS, countSharded, shardNanos / 1e6, countSharded == countSingle ? "YES" : "NO");
        System.out.println();
    }

    /** E3：区域拆分（设备前缀 = 地市码）：单区域命中单片、全局跨片聚合。 */
    private void e3RegionShard() {
        String[] prefixes = {"4101", "4102", "4103", "4104"}; // 4 个地市码
        RegionSharder sharder = new RegionSharder(prefixes);
        long perRegion = 300_000L;
        int regions = prefixes.length;

        BitMap[] byRegion = new BitMap[regions];
        for (int s = 0; s < regions; s++) {
            byRegion[s] = new BitMap(perRegion);
            for (long i = 0; i < perRegion; i += 4) {
                byRegion[s].setBit(i);
            }
        }
        BitMap single = new BitMap(perRegion * regions);
        for (int s = 0; s < regions; s++) {
            for (long i = 0; i < perRegion; i += 4) {
                single.setBit(s * perRegion + i);
            }
        }

        long t0 = System.nanoTime();
        long region0 = byRegion[0].bitCount();
        long regionNanos = System.nanoTime() - t0;
        t0 = System.nanoTime();
        long singleScan = single.bitCount();
        long singleNanos = System.nanoTime() - t0;

        t0 = System.nanoTime();
        long global = 0;
        for (BitMap m : byRegion) {
            global += m.bitCount();
        }
        long globalNanos = System.nanoTime() - t0;

        System.out.printf("  E3 区域拆分（4 个地市 × %,d 设备）:%n", perRegion);
        System.out.printf("    单区域统计: 分片命中单片 %,d (%.2f ms) | 单 key 需全量扫 %,d 再过滤 (%.2f ms)%n",
                region0, regionNanos / 1e6, singleScan, singleNanos / 1e6);
        System.out.printf("    全局统计  : 分片跨片聚合 %,d (%d 片求和, %.2f ms) | 单 key 一次 %,d%n%n",
                global, regions, globalNanos / 1e6, single.bitCount());
    }

    /** E4：增量计数——SETBIT 时维护 count，在线数 O(1) 直读。 */
    private void e4IncrementalCounter() {
        BitMapWithCounter online = new BitMapWithCounter(BIG_KEY_BITS);
        for (long i = 0; i < BIG_KEY_BITS; i += 1000) {
            online.setBit(i);
        }
        long t0 = System.nanoTime();
        long counter = online.getCount();
        long counterNanos = System.nanoTime() - t0;
        t0 = System.nanoTime();
        long bitcount = online.bitCount();
        long bitcountNanos = System.nanoTime() - t0;

        System.out.printf("  E4 增量计数（%,d 位）:%n", BIG_KEY_BITS);
        System.out.printf("    增量计数 O(1)    : %,d, 耗时 %.2f ms%n", counter, counterNanos / 1e6);
        System.out.printf("    BITCOUNT O(N/64) : %,d, 耗时 %.2f ms%n", bitcount, bitcountNanos / 1e6);
        System.out.printf("    一致性           : %s%n", counter == bitcount ? "count == BITCOUNT OK" : "MISMATCH");
    }
}
