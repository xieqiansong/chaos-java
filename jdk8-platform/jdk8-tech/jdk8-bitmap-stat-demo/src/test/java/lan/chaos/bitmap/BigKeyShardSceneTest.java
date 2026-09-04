package lan.chaos.bitmap;

import lan.chaos.bitmap.bigkey.BitMapWithCounter;
import lan.chaos.bitmap.bigkey.HashSharder;
import lan.chaos.bitmap.bigkey.RegionSharder;
import lan.chaos.bitmap.bigkey.SingleThreadQueueSimulator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 大 key 拆分优化测试：哈希/区域分片、增量计数一致性、单线程尾延迟改善。
 */
public class BigKeyShardSceneTest {

    @Test
    public void hashShardIsDeterministic() {
        HashSharder sharder = new HashSharder(8);
        for (String id : new String[]{"dev-10001", "dev-99999", "dev-abc123"}) {
            int first = sharder.shardOf(id);
            for (int i = 0; i < 5; i++) {
                assertEquals(first, sharder.shardOf(id));
            }
        }
    }

    @Test
    public void hashShardCoversAllShards() {
        HashSharder sharder = new HashSharder(8);
        boolean[] seen = new boolean[8];
        for (long i = 0; i < 10_000; i++) {
            seen[sharder.shardOf("dev-" + i)] = true;
        }
        for (boolean b : seen) {
            assertTrue(b, "每个分片都应有设备落入");
        }
    }

    @Test
    public void regionShardByPrefix() {
        RegionSharder sharder = new RegionSharder(new String[]{"4101", "4102", "4103"});
        assertEquals(0, sharder.shardOf("410100001"));
        assertEquals(1, sharder.shardOf("410299999"));
        assertEquals(2, sharder.shardOf("410312345"));
        assertEquals(sharder.regionOf(1), "4102");
    }

    @Test
    public void regionShardUnknownPrefixThrows() {
        RegionSharder sharder = new RegionSharder(new String[]{"4101", "4102"});
        try {
            sharder.shardOf("000099999");
            fail("未知前缀应抛异常");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void incrementalCounterMatchesBitCount() {
        BitMapWithCounter bm = new BitMapWithCounter(1_000_000);
        for (long i = 0; i < 1_000_000; i += 3) {
            bm.setBit(i);
        }
        assertEquals(bm.bitCount(), bm.getCount());
        // 0->1 与 1->0 翻转后仍一致
        bm.clearBit(0);
        assertEquals(bm.bitCount(), bm.getCount());
        bm.setBit(0);
        assertEquals(bm.bitCount(), bm.getCount());
        // 重复置位不计入
        long before = bm.getCount();
        bm.setBit(3);
        assertEquals(before, bm.getCount());
    }

    @Test
    public void splittingReducesTailLatency() {
        long bigCost = SingleThreadQueueSimulator.bitCountCost(400_000_000L);
        long[] pieces = SingleThreadQueueSimulator.splitCost(bigCost, 8);
        assertEquals(8, pieces.length);
        assertEquals(bigCost, sum(pieces));

        int n = 100;
        long[] single = new long[n + 1];
        Arrays.fill(single, 1);
        single[n / 2] = bigCost; // 第 50 个命令是大 key BITCOUNT
        long tailSingle = SingleThreadQueueSimulator.finishTimes(single)[n / 2 + 1];

        long[] split = new long[n + pieces.length];
        Arrays.fill(split, 1);
        System.arraycopy(pieces, 0, split, n / 2, pieces.length);
        long tailSplit = SingleThreadQueueSimulator.finishTimes(split)[n / 2 + 1];

        assertEquals(bigCost + (n / 2 + 1), tailSingle);
        assertTrue(tailSplit < tailSingle, "拆分后第 51 个命令的等待应显著缩短");
    }

    private static long sum(long[] arr) {
        long s = 0;
        for (long v : arr) {
            s += v;
        }
        return s;
    }
}
