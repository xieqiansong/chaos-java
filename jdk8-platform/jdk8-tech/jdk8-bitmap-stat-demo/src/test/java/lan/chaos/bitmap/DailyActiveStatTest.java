package lan.chaos.bitmap;

import lan.chaos.bitmap.core.BitMap;
import lan.chaos.bitmap.stat.DailyActiveStat;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 日活统计测试：BITOP OR（近 N 天活跃）与 BITOP AND（连续 N 天活跃）语义。
 */
public class DailyActiveStatTest {

    @Test
    public void unionAndIntersectOfDays() {
        // day0: {0,2,3,4}  day1: {0,3,6}  day2: {1,3,5}
        BitMap[] days = {
                bitmapOf(10, 0, 2, 3, 4),
                bitmapOf(10, 0, 3, 6),
                bitmapOf(10, 1, 3, 5),
        };
        // 并集 = {0,1,2,3,4,5,6} → 7；交集 = {3} → 1
        assertEquals(7, DailyActiveStat.unionActiveCount(days));
        assertEquals(1, DailyActiveStat.intersectActiveCount(days));
    }

    @Test
    public void singleDay() {
        BitMap[] days = {bitmapOf(8, 1, 5)};
        assertEquals(2, DailyActiveStat.unionActiveCount(days));
        assertEquals(2, DailyActiveStat.intersectActiveCount(days));
    }

    @Test
    public void emptyDaysReturnsZero() {
        assertEquals(0, DailyActiveStat.unionActiveCount(new BitMap[0]));
        assertEquals(0, DailyActiveStat.intersectActiveCount(new BitMap[0]));
    }

    private static BitMap bitmapOf(long cap, long... bits) {
        BitMap bm = new BitMap(cap);
        for (long b : bits) {
            bm.setBit(b);
        }
        return bm;
    }
}
