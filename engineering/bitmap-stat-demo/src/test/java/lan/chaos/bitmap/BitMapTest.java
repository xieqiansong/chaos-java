package lan.chaos.bitmap;

import lan.chaos.bitmap.core.BitMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 核心位图单元测试：置位/清零/查询、跨 long 边界、自动扩容、BITCOUNT、BITOP、内存占用。
 */
public class BitMapTest {

    @Test
    public void setGetClearBasic() {
        BitMap bm = new BitMap(1024);
        assertFalse(bm.getBit(0));
        bm.setBit(0);
        assertTrue(bm.getBit(0));
        bm.clearBit(0);
        assertFalse(bm.getBit(0));
    }

    @Test
    public void crossWordBoundary() {
        BitMap bm = new BitMap(1024);
        bm.setBit(63);
        bm.setBit(64);
        assertTrue(bm.getBit(63));
        assertTrue(bm.getBit(64));
        assertFalse(bm.getBit(65));
    }

    @Test
    public void bigOffsetAutoGrow() {
        BitMap bm = new BitMap(64);
        long off = 1_000_000L; // 远超初始容量
        bm.setBit(off);
        assertTrue(bm.getBit(off));
        assertFalse(bm.getBit(off - 1));
        assertTrue(bm.capacityBits() >= off + 1);
    }

    @Test
    public void getBitOutOfRangeReturnsFalse() {
        BitMap bm = new BitMap(64);
        assertFalse(bm.getBit(64));
        assertFalse(bm.getBit(-1));
    }

    @Test
    public void bitCount() {
        BitMap bm = new BitMap(256);
        for (int i = 0; i < 100; i += 7) {
            bm.setBit(i);
        }
        assertEquals(15, bm.bitCount()); // 0,7,14,...,98
    }

    @Test
    public void bitOpOr() {
        BitMap a = new BitMap(128);
        BitMap b = new BitMap(128);
        a.setBit(1);
        a.setBit(3);
        b.setBit(2);
        b.setBit(3);
        BitMap r = a.bitOpOr(b);
        assertTrue(r.getBit(1));
        assertTrue(r.getBit(2));
        assertTrue(r.getBit(3));
        assertFalse(r.getBit(4));
        assertEquals(3, r.bitCount());
        // 原对象不被修改
        assertFalse(a.getBit(2));
        assertFalse(b.getBit(1));
    }

    @Test
    public void bitOpAnd() {
        BitMap a = new BitMap(128);
        BitMap b = new BitMap(128);
        a.setBit(1);
        a.setBit(3);
        b.setBit(2);
        b.setBit(3);
        BitMap r = a.bitOpAnd(b);
        assertTrue(r.getBit(3));
        assertFalse(r.getBit(1));
        assertFalse(r.getBit(2));
        assertEquals(1, r.bitCount());
    }

    @Test
    public void bitOpDifferentLengths() {
        BitMap a = new BitMap(64);
        BitMap b = new BitMap(200);
        a.setBit(1);
        b.setBit(150);
        BitMap or = a.bitOpOr(b);
        assertTrue(or.getBit(1));
        assertTrue(or.getBit(150));
        assertEquals(2, or.bitCount());
        BitMap and = a.bitOpAnd(b);
        assertEquals(0, and.bitCount());
    }

    @Test
    public void memoryUsage() {
        // 1200 万设备 = 187,500 个 long = 1,500,000 字节 ≈ 1.43 MiB
        BitMap bm = new BitMap(12_000_000L);
        assertEquals(12_000_000L / 64 * 8, bm.memoryBytes());
        assertEquals(1_500_000L, bm.memoryBytes());
    }
}
