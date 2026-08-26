package lan.chaos.bitmap.bigkey;

import lan.chaos.bitmap.core.BitMap;

/**
 * 带增量计数的位图：SETBIT 0-&gt;1 时 count++、1-&gt;0 时 count--，
 * 在线数/活跃数 O(1) 直读，完全绕开 BITCOUNT 的 O(N) 全量扫描。
 *
 * <p>生产形态：Redis 上用独立 counter 字段（或 BITFIELD），SETBIT 与计数更新同事务；
 * 代价是每次状态变更多一次计数维护，适合「查询远多于变更」的在线/日活场景。</p>
 */
public class BitMapWithCounter extends BitMap {

    private long count;

    public BitMapWithCounter(long capacityBits) {
        super(capacityBits);
    }

    @Override
    public void setBit(long offset, boolean value) {
        boolean old = getBit(offset);
        super.setBit(offset, value);
        if (old != value) {
            count += value ? 1 : -1;
        }
    }

    /** O(1) 获取置位总数（与 BITCOUNT 结果保持一致）。 */
    public long getCount() {
        return count;
    }
}
