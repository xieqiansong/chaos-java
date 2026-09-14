package lan.chaos.bitmap.stat;

import lan.chaos.bitmap.core.BitMap;
import lan.chaos.bitmap.probabilistic.HyperLogLog;

import java.util.Random;

/**
 * 场景 F：UV 统计对比——Bitmap 精确 vs HyperLogLog 近似。
 *
 * <p>承接 1200 万设备场景：某日 300 万设备活跃，产生 600 万条「当日访问」（含重复访问）。
 * Bitmap 对设备 ID 精确去重得到精确 UV（内存 1.43 MB，需预知基数上界）；
 * HLL 逐条 pfadd 后估算 UV（内存恒定 ~16 KB），以 ~0.81% 误差换恒定内存与无上界。</p>
 *
 * <p>取舍结论：精确度敏感的统计（在线状态、单设备维度）用 Bitmap；
 * 只关心量级的统计（全站 UV、日活量级）用 HLL——这正是「按需选数据结构」的落点。</p>
 */
public final class UvStatCompare {

    private static final long DEVICES = 12_000_000L;
    private static final long ACTIVE_UV = 3_000_000L;
    private static final long ACCESS_RECORDS = 6_000_000L;

    public void run() {
        System.out.println("== 场景 F：UV 统计对比（Bitmap 精确 vs HLL 近似，1200 万设备） ==");

        Random rnd = new Random(42L); // 固定种子，输出可复现
        BitMap uvBitmap = new BitMap(DEVICES);
        HyperLogLog hll = new HyperLogLog();

        // 造 600 万条「当日访问」：设备 ID 从 300 万活跃集内抽样（重复访问不增 UV）
        for (long i = 0; i < ACCESS_RECORDS; i++) {
            long deviceId = rnd.nextInt((int) ACTIVE_UV);
            uvBitmap.setBit(deviceId);
            hll.pfadd("dev-" + deviceId);
        }

        long exactUv = uvBitmap.bitCount();
        long approxUv = hll.pfcount();
        double errPct = Math.abs(approxUv - exactUv) * 100.0 / exactUv;

        System.out.printf("  精确 UV (Bitmap BITCOUNT) : %,d%n", exactUv);
        System.out.printf("  估算 UV (HLL PFCOUNT)     : %,d%n", approxUv);
        System.out.printf("  误差率                    : %.2f%%（p=14 理论 ~0.81%%）%n", errPct);
        System.out.printf("  Bitmap 内存               : %,d B ≈ %.2f MB（1 bit/设备，需预知基数上界）%n",
                uvBitmap.memoryBytes(), uvBitmap.memoryBytes() / 1024.0 / 1024.0);
        System.out.printf("  HLL 内存                  : %,d B ≈ %.2f KB（16384 寄存器，与基数无关）%n",
                hll.memoryBytes(), hll.memoryBytes() / 1024.0);
        System.out.printf("  HLL 打包后（6-bit/寄存器）: %,d B ≈ %.2f KB（Redis 式）%n",
                hll.packedBytes(), hll.packedBytes() / 1024.0);
        System.out.println();
    }
}
