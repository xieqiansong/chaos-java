package lan.chaos.bitmap.probabilistic;

import java.nio.charset.StandardCharsets;

/**
 * 自实现 HyperLogLog（纯 JDK8、零依赖），模拟 Redis {@code PFADD}/{@code PFCOUNT} 的核心语义。
 *
 * <p><b>解决什么痛点</b>：Bitmap 统计 UV 精确，但内存随基数上界线性增长
 * （1200 万设备 = 1.43 MB）；而日活/UV 这类「只关心量级、不要求绝对精确」的指标，
 * 用 HLL 以 ~0.81% 误差换 <b>恒定 ~12 KB 内存</b>，且不需要预先知道基数上界。</p>
 *
 * <p><b>原理</b>：把元素 hash 成 64 位，取低 p 位定位到 m = 2^p 个寄存器之一；
 * 其余位从最高位起数「连续 0 的个数 + 1」作为 rank 写入该寄存器（保留历史最大 rank）。
 * 为什么能估算基数：hash 均匀 → 某寄存器 rank &ge; k 的概率 ≈ 2^-k，
 * 全量寄存器调和平均后 ≈ 1 / 基数，反推即得估计值。</p>
 *
 * <p><b>误差</b>：标准误差 ≈ 1.04 / sqrt(m)，只与寄存器数 m 相关、与数据规模无关：
 * p=14（m=16384）≈ 0.81%，p=10（m=1024）≈ 3.25%。</p>
 *
 * <p><b>生产坑</b>：
 * <ul>
 *   <li>小基数（&lt; 2.5m）直接用调和平均会系统性低估，需 LinearCounting 修正；</li>
 *   <li>hash 质量决定准确度上限，必须用均匀性好的散列（本类用 MurmurHash3 x64），
 *       {@link String#hashCode} 分布不够好不可用；</li>
 *   <li>Redis 对寄存器做 6-bit 打包：16384 × 6 bit = 12288 B ≈ 12 KB。
 *       本实现为便于理解每寄存器 1 字节（16 KB），语义等价。</li>
 * </ul></p>
 */
public final class HyperLogLog {

    private static final double ALPHA_16384 = 0.7213 / (1.0 + 1.079 / 16384.0);
    private static final double SMALL_RANGE = 2.5;

    private final int p;
    private final int m;
    private final byte[] registers; // 每寄存器 1 字节（真实实现 6-bit 打包）

    public HyperLogLog() {
        this(14); // 默认 p=14：16384 寄存器，误差 ~0.81%
    }

    public HyperLogLog(int p) {
        if (p < 4 || p > 30) {
            throw new IllegalArgumentException("p must be in [4,30], got " + p);
        }
        this.p = p;
        this.m = 1 << p;
        this.registers = new byte[m];
    }

    /** PFADD：向基数统计中增量插入一个元素（重复插入不膨胀计数）。 */
    public void pfadd(String element) {
        long h = murmur3(element);
        int index = (int) (h & (m - 1));      // 低 p 位 → 寄存器下标
        long w = h >>> p;                     // 高 (64-p) 位 → 数 rank
        // numberOfLeadingZeros 是 64 位视角，而 w 只有 (64-p) 位有效（高 p 位恒为 0），
        // 需减掉 p 个冗余前导 0，否则 rank 系统性偏大 p、估算值放大 2^p 倍
        int rank = w == 0 ? 64 - p + 1 : 1 + Long.numberOfLeadingZeros(w) - p;
        if (rank > registers[index]) {
            registers[index] = (byte) rank;
        }
    }

    /**
     * PFCOUNT：估算当前基数。
     *
     * <p>调和平均：E = α·m² / Σ 2^(-reg)。当 E 落在小基数区间（&le; 2.5m）且存在空寄存器时，
     * 用 LinearCounting（E = m·ln(m/V)，V 为空寄存器数）修正，避免系统性低估。</p>
     */
    public long pfcount() {
        double sum = 0;
        int zeros = 0;
        for (byte b : registers) {
            int v = b & 0xff;
            sum += 1.0 / (1L << v);
            if (v == 0) {
                zeros++;
            }
        }
        double estimate = ALPHA_16384 * m * m / sum;
        if (estimate <= SMALL_RANGE * m && zeros > 0) {
            estimate = m * Math.log((double) m / zeros); // LinearCounting 小基数修正
        }
        return Math.round(estimate);
    }

    /** 本实现占用堆内存字节数（每寄存器 1 字节）。 */
    public int memoryBytes() {
        return registers.length;
    }

    /** Redis 式 6-bit 打包的理论字节数（16384 × 6 / 8 = 12288 B ≈ 12 KB）。 */
    public int packedBytes() {
        return m * 6 / 8;
    }

    /** 寄存器个数（m = 2^p），供场景输出/测试使用。 */
    public int registerCount() {
        return m;
    }

    /** MurmurHash3 x64 128 的 low 64 位，seed=0。分布均匀，适合做 HLL 散列。 */
    private static long murmur3(String key) {
        byte[] data = key.getBytes(StandardCharsets.UTF_8);
        long h1 = 0L, h2 = 0L;
        int len = data.length;
        int i = 0;
        while (i + 16 <= len) {
            long k1 = readLongLE(data, i);
            long k2 = readLongLE(data, i + 8);
            i += 16;

            k1 *= 0x87c37b91114253d5L;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= 0x4cf5ad432745937fL;
            h1 ^= k1;
            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729L;

            k2 *= 0x4cf5ad432745937fL;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= 0x87c37b91114253d5L;
            h2 ^= k2;
            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5L;
        }

        long k1 = 0L, k2 = 0L;
        int tail = len - i;
        switch (tail) {
            case 15: k2 ^= (long) (data[i + 14] & 0xff) << 48;
            case 14: k2 ^= (long) (data[i + 13] & 0xff) << 40;
            case 13: k2 ^= (long) (data[i + 12] & 0xff) << 32;
            case 12: k2 ^= (long) (data[i + 11] & 0xff) << 24;
            case 11: k2 ^= (long) (data[i + 10] & 0xff) << 16;
            case 10: k2 ^= (long) (data[i + 9] & 0xff) << 8;
            case 9:  k2 ^= (long) (data[i + 8] & 0xff);
                k2 *= 0x4cf5ad432745937fL;
                k2 = Long.rotateLeft(k2, 33);
                k2 *= 0x87c37b91114253d5L;
                h2 ^= k2;
            case 8:  k1 ^= (long) (data[i + 7] & 0xff) << 56;
            case 7:  k1 ^= (long) (data[i + 6] & 0xff) << 48;
            case 6:  k1 ^= (long) (data[i + 5] & 0xff) << 40;
            case 5:  k1 ^= (long) (data[i + 4] & 0xff) << 32;
            case 4:  k1 ^= (long) (data[i + 3] & 0xff) << 24;
            case 3:  k1 ^= (long) (data[i + 2] & 0xff) << 16;
            case 2:  k1 ^= (long) (data[i + 1] & 0xff) << 8;
            case 1:  k1 ^= (long) (data[i] & 0xff);
                k1 *= 0x87c37b91114253d5L;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= 0x4cf5ad432745937fL;
                h1 ^= k1;
            default:
        }

        h1 ^= len;
        h2 ^= len;
        h1 += h2;
        h2 += h1;
        h1 = fmix64(h1);
        h2 = fmix64(h2);
        h1 += h2;
        return h1; // 128 位结果取 low 64 位
    }

    private static long readLongLE(byte[] b, int off) {
        long v = 0;
        for (int i = 7; i >= 0; i--) {
            v = (v << 8) | (b[off + i] & 0xff);
        }
        return v;
    }

    private static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }
}
