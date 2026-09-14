package lan.chaos.bitmap.stat;

import lan.chaos.bitmap.core.BitMap;

/**
 * 场景 A：在线状态维护（1200 万设备、1 bit/设备）。
 * 上线 SETBIT、下线清零、实时在线数 BITCOUNT、任意设备在线查询 GETBIT（O(1)）。
 */
public final class OnlineStatusStat {

    private static final long DEVICES = 12_000_000L;

    public void run() {
        System.out.println("== 场景 A：在线状态维护（1200 万设备） ==");
        BitMap online = new BitMap(DEVICES);
        // 模拟 300 万设备上线：每第 4 台在线
        for (long dev = 0; dev < DEVICES; dev += 4) {
            online.setBit(dev);
        }

        long t0 = System.nanoTime();
        long onlineCount = online.bitCount();
        long bitcountNanos = System.nanoTime() - t0;

        System.out.printf("  总设备数            : %,d%n", DEVICES);
        System.out.printf("  在线设备数(BITCOUNT) : %,d（期望 %,d）%n", onlineCount, DEVICES / 4);
        System.out.printf("  位图内存占用        : %,d 字节 ≈ %.2f MB（1 bit/设备）%n",
                online.memoryBytes(), online.memoryBytes() / 1024.0 / 1024.0);
        System.out.printf("  GETBIT 抽查         : dev-1=%s dev-4=%s dev-%d=%s%n",
                online.getBit(1), online.getBit(4), DEVICES - 1, online.getBit(DEVICES - 1));
        System.out.printf("  BITCOUNT 耗时       : %.2f ms%n", bitcountNanos / 1e6);
        System.out.println();
    }
}
