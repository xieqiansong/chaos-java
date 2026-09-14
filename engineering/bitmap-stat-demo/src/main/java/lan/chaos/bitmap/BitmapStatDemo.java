package lan.chaos.bitmap;

import lan.chaos.bitmap.bigkey.BigKeyShardScene;
import lan.chaos.bitmap.core.BitMapBenchmark;
import lan.chaos.bitmap.stat.DailyActiveStat;
import lan.chaos.bitmap.stat.MemoryAccountStat;
import lan.chaos.bitmap.stat.OnlineStatusStat;
import lan.chaos.bitmap.stat.UvStatCompare;

/**
 * Bitmap 统计 Demo 入口：串行演示六个场景。
 *
 * <pre>
 *   场景 A：在线状态维护（SETBIT/GETBIT/BITCOUNT，1200 万设备）
 *   场景 B：日活统计（按天位图 + BITOP OR/AND）
 *   场景 C：内存账对比（Bitmap vs byte[] vs Set&lt;String&gt;）
 *   场景 D：吞吐对比（GETBIT vs Set.contains，BITCOUNT vs Set.size）
 *   场景 E：大 key 位运算瓶颈与拆分优化（单线程阻塞/哈希分片/区域分片/增量计数）
 *   场景 F：UV 统计对比（Bitmap 精确 vs HyperLogLog 近似）
 * </pre>
 */
public final class BitmapStatDemo {

    public static void main(String[] args) {
        System.out.println("== Bitmap 统计 Demo（纯 JDK8，零运行时依赖） ==");
        System.out.println();

        new OnlineStatusStat().run();
        new DailyActiveStat().run();
        new MemoryAccountStat().run();
        new BitMapBenchmark(12_000_000L, 3_000_000L, 20_000_000).run();
        new BigKeyShardScene().run();
        new UvStatCompare().run();

        System.out.println("== 全部场景演示完成 ==");
    }
}
