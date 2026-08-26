package lan.chaos.bitmap.core;

import java.util.Arrays;

/**
 * 自实现 long[] 位图，模拟 Redis Bitmap 的核心语义（SETBIT / GETBIT / BITCOUNT / BITOP AND|OR）。
 *
 * <p>底层换算：{@code words[offset >>> 6]} 定位所在 long（一个 long 存 64 位），
 * {@code 1L << (offset & 63)} 定位具体位。与 Redis 的 byte 粒度（offset &gt;&gt;&gt; 3、1 &lt;&lt; (offset &amp; 7)）
 * 同一思路，只是换成 64 位宽、位运算吞吐更高。</p>
 *
 * <p>为什么省内存：每个设备只占 1 bit，1200 万设备 ≈ 1.43 MB；BITCOUNT 用 CPU 的
 * {@link Long#bitCount} 逐 long 累加，内存连续、cache 友好。</p>
 */
public class BitMap {

    private long[] words;
    private long capacityBits;

    public BitMap(long capacityBits) {
        if (capacityBits < 0) {
            throw new IllegalArgumentException("capacityBits must be >= 0, got " + capacityBits);
        }
        this.capacityBits = capacityBits;
        this.words = new long[wordCount(capacityBits)];
    }

    private static int wordCount(long bits) {
        long wc = (bits + 63) >>> 6;
        if (wc > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("too many bits: " + bits);
        }
        return (int) wc;
    }

    /** SETBIT：将 offset 位置为 1。 */
    public void setBit(long offset) {
        setBit(offset, true);
    }

    /** 将 offset 位清零。 */
    public void clearBit(long offset) {
        setBit(offset, false);
    }

    /**
     * SETBIT / 清零（0-&gt;1 与 1-&gt;0）。
     * BitMapWithCounter 覆写本方法做增量计数，因此 setBit(long)/clearBit(long) 也会走计数。
     */
    public void setBit(long offset, boolean value) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0, got " + offset);
        }
        int idx = ensureCapacity(offset);
        long mask = 1L << (offset & 63);
        if (value) {
            words[idx] |= mask;
        } else {
            words[idx] &= ~mask;
        }
    }

    /** GETBIT：查询 offset 位是否为 1，超出容量返回 false。 */
    public boolean getBit(long offset) {
        if (offset < 0 || offset >= capacityBits) {
            return false;
        }
        int idx = wordIndex(offset);
        if (idx >= words.length) {
            return false;
        }
        return (words[idx] & (1L << (offset & 63))) != 0;
    }

    /** BITCOUNT：统计置位个数，等价 Redis {@code BITCOUNT key}。 */
    public long bitCount() {
        long n = 0;
        for (long w : words) {
            n += Long.bitCount(w);
        }
        return n;
    }

    /** BITOP OR：返回新位图，长度取两方较长，不足部分按 0 处理。 */
    public BitMap bitOpOr(BitMap other) {
        BitMap result = new BitMap(Math.max(capacityBits, other.capacityBits));
        for (int i = 0; i < result.words.length; i++) {
            long a = i < words.length ? words[i] : 0L;
            long b = i < other.words.length ? other.words[i] : 0L;
            result.words[i] = a | b;
        }
        return result;
    }

    /** BITOP AND：返回新位图，任一方为 0 则结果为 0。 */
    public BitMap bitOpAnd(BitMap other) {
        BitMap result = new BitMap(Math.max(capacityBits, other.capacityBits));
        for (int i = 0; i < result.words.length; i++) {
            long a = i < words.length ? words[i] : 0L;
            long b = i < other.words.length ? other.words[i] : 0L;
            result.words[i] = a & b;
        }
        return result;
    }

    /** 逻辑位容量。 */
    public long capacityBits() {
        return capacityBits;
    }

    /** 底层数组实际占用堆内存字节数。 */
    public long memoryBytes() {
        return words.length * 8L;
    }

    private int wordIndex(long offset) {
        return (int) (offset >>> 6);
    }

    private int ensureCapacity(long offset) {
        int idx = wordIndex(offset);
        if (idx >= words.length) {
            int newLen = wordCount(offset + 1);
            words = Arrays.copyOf(words, newLen);
            capacityBits = offset + 1;
        }
        return idx;
    }
}
