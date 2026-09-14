package lan.chaos.bitmap.stat;

/**
 * 场景 C：内存账对比（1200 万设备）。
 * 静态容量账：Bitmap 1 bit/设备 vs byte[] 1 B/设备 vs Set&lt;String&gt; 每对象数十~上百字节。
 */
public final class MemoryAccountStat {

    private static final long DEVICES = 12_000_000L;

    public void run() {
        System.out.println("== 场景 C：内存账对比（1200 万设备） ==");
        long bitmap = (DEVICES + 7) / 8;
        long byteArray = DEVICES;
        long set = DEVICES * 104L;

        System.out.printf("  %-20s %10s %13s  %s%n", "方案", "单设备", "1200 万占用", "说明");
        System.out.printf("  %-20s %10s %13s  %s%n", "--------------------", "--------", "-------------", "--------------------");
        System.out.printf("  %-20s %10s %10.2f MB  %s%n", "Bitmap(1 bit/设备)", "1 bit", bitmap / 1024.0 / 1024.0, "连续内存, cache 友好");
        System.out.printf("  %-20s %10s %10.2f MB  %s%n", "byte[](1 B/设备)", "1 B", byteArray / 1024.0 / 1024.0, "状态/温度等小整型");
        System.out.printf("  %-20s %10s %10.2f MB  %s%n", "Set<String>(估算)", "≈104 B", set / 1024.0 / 1024.0,
                "String(15字符≈72B) + HashMap.Node(≈32B)");
        System.out.println();
    }
}
