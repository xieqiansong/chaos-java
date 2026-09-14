package lan.chaos.bitmap;

import lan.chaos.bitmap.probabilistic.HyperLogLog;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HyperLogLog 单元测试：基数估算误差带（小/中/大基数）、去重语义、恒定内存。
 */
public class HyperLogLogTest {

    /** 固定种子，保证估算结果可复现。 */
    private static final long SEED = 42L;

    private static double relErrPct(HyperLogLog hll, long exact) {
        return Math.abs(hll.pfcount() - exact) * 100.0 / exact;
    }

    @Test
    public void emptyCardinalityIsZero() {
        assertEquals(0L, new HyperLogLog().pfcount());
    }

    @Test
    public void smallCardinalityWithinError() {
        // 小基数区间依赖 LinearCounting 修正，误差应显著小于直接调和平均
        int n = 1_000;
        HyperLogLog hll = new HyperLogLog();
        Random rnd = new Random(SEED);
        for (int i = 0; i < n; i++) {
            hll.pfadd("dev-" + rnd.nextInt(10_000_000));
        }
        assertTrue(relErrPct(hll, n) < 10.0, "小基数误差过大: " + relErrPct(hll, n) + "%");
    }

    @Test
    public void largeCardinalityWithinError() {
        // 300 万唯一设备，p=14 理论误差 ~0.81%，留 2% 余量
        int n = 3_000_000;
        HyperLogLog hll = new HyperLogLog();
        Random rnd = new Random(SEED);
        for (int i = 0; i < n; i++) {
            hll.pfadd("dev-" + i);
        }
        assertTrue(relErrPct(hll, n) < 2.0, "大基数误差过大: " + relErrPct(hll, n) + "%");
    }

    @Test
    public void duplicatesDoNotInflate() {
        // 100 万唯一 + 重复插入 500 万次（模拟重复访问），估算不应显著膨胀
        int unique = 1_000_000;
        HyperLogLog hll = new HyperLogLog();
        Random rnd = new Random(SEED);
        for (int i = 0; i < unique; i++) {
            hll.pfadd("dev-" + i);
        }
        for (int i = 0; i < 5_000_000; i++) {
            hll.pfadd("dev-" + rnd.nextInt(unique)); // 全部是重复元素
        }
        assertTrue(relErrPct(hll, unique) < 2.0, "重复插入导致膨胀: " + relErrPct(hll, unique) + "%");
    }

    @Test
    public void memoryIsConstantRegardlessOfCardinality() {
        // 内存只由寄存器数决定（p=14 → 16384 字节），与已插入元素个数无关
        HyperLogLog empty = new HyperLogLog();
        HyperLogLog filled = new HyperLogLog();
        Random rnd = new Random(SEED);
        for (int i = 0; i < 100_000; i++) {
            filled.pfadd("dev-" + i);
        }
        assertEquals(16384, empty.memoryBytes());
        assertEquals(empty.memoryBytes(), filled.memoryBytes());
        assertEquals(12288, filled.packedBytes()); // Redis 式 6-bit 打包 ≈ 12 KB
    }
}
