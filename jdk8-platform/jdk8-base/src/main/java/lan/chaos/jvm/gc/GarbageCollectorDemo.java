package lan.chaos.jvm.gc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * 能力一：JVM 垃圾回收器对比与调优参数实战。
 *
 * <p>WHY：GC 是 JVM 最核心的子系统之一。理解不同 GC 算法的适用场景和调优参数，
 * 是线上性能排障和 JVM 调优的必备技能。错误的 GC 选型可能导致：
 * <ul>
 *   <li>Serial GC 用于高吞吐服务 → STW 时间过长，服务假死</li>
 *   <li>CMS 在 JDK8 下碎片化 → Concurrent Mode Failure 后降级为 Serial Old，雪崩</li>
 *   <li>G1 在堆过大时 Mixed GC 耗时长 → 需调优 IHOP/MaxGCPauseMillis</li>
 * </ul>
 *
 * <p>关键知识点：
 * <ul>
 *   <li>GC 分代：Young（Eden+S0+S1）Minor GC / Old（Full GC）</li>
 *   <li>JDK8 四大 GC：Serial（-XX:+UseSerialGC）/ Parallel（默认，-XX:+UseParallelGC）
 *       / CMS（-XX:+UseConcMarkSweepGC，已废弃）/ G1（-XX:+UseG1GC）</li>
 *   <li>关键 JVM 参数：-Xms/-Xmx（堆大小）、-Xmn（新生代）、-XX:SurvivorRatio、
 *       -XX:MaxTenuringThreshold、-XX:+PrintGCDetails</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li>别把 -Xms 和 -Xmx 设成不相等 → 堆内存频繁伸缩会触发 FGC</li>
 *   <li>大对象（byte[] > 几十 MB）直接进老年代  → 用 -XX:PretenureSizeThreshold 控制</li>
 *   <li>不要在生产环境盲目开所有 GC 日志（-Xlog:gc*），IO 开销大</li>
 * </ul>
 */
public class GarbageCollectorDemo {

    /**
     * Step 1：探测当前 JVM 使用的 GC 类型和内存布局。
     * 输出当前运行环境的 GC 名称、堆内存分区大小。
     */
    public String detectCurrentGC() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 1. 当前 JVM GC 探測 ===\n");

        // GC 收集器信息
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        sb.append("GC 收集器:\n");
        for (GarbageCollectorMXBean gc : gcBeans) {
            sb.append("  ").append(gc.getName())
                    .append(" (collections=").append(gc.getCollectionCount())
                    .append(", time=").append(gc.getCollectionTime()).append("ms)\n");
        }

        // 内存分区信息
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        sb.append("\n堆内存:\n");
        sb.append("  init=").append(heap.getInit() / 1024 / 1024).append("MB")
                .append(", used=").append(heap.getUsed() / 1024 / 1024).append("MB")
                .append(", committed=").append(heap.getCommitted() / 1024 / 1024).append("MB")
                .append(", max=").append(heap.getMax() / 1024 / 1024).append("MB\n");

        // 非堆
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        sb.append("非堆(元空间/CodeCache):\n");
        sb.append("  init=").append(nonHeap.getInit() / 1024 / 1024).append("MB")
                .append(", used=").append(nonHeap.getUsed() / 1024 / 1024).append("MB")
                .append(", committed=").append(nonHeap.getCommitted() / 1024 / 1024).append("MB\n");

        return sb.toString();
    }

    /**
     * Step 2：模拟 Minor GC —— 快速创建大量短生命周期对象填满 Eden 区。
     * <p>Eden 区满 → Minor GC → 存活对象进入 Survivor 区 → 反复多次 Survivor 晋升老年代。
     * 观察 GC 次数和内存使用变化。
     */
    public String simulateMinorGC() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long gcCountBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();

        // 在 Eden 区创建大量短命对象，触发 Minor GC
        int objectCount = 500_000;
        List<byte[]> tempObjects = new ArrayList<>();
        for (int i = 0; i < objectCount; i++) {
            tempObjects.add(new byte[1024]); // 每个 1KB
        }
        tempObjects.clear(); // 立即释放，让 GC 回收
        System.gc(); // 建议 GC

        long gcCountAfter = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long heapUsedAfter = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;

        StringBuilder sb = new StringBuilder();
        sb.append("=== 2. Minor GC 模拟 ===\n");
        sb.append("创建并释放 ").append(objectCount).append(" 个 1KB 对象 (共 ~").append(objectCount / 1024).append("MB)\n");
        sb.append("GC 次数变化: ").append(gcCountBefore).append(" → ").append(gcCountAfter)
                .append(" (").append(gcCountAfter >= gcCountBefore ? "触发了 GC" : "?").append(")\n");
        sb.append("当前堆使用: ").append(heapUsedAfter).append("MB / max ")
                .append(memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024).append("MB\n");
        sb.append("说明: Eden 区填满 → Minor GC → 无引用对象被回收，Eden 清空\n");
        return sb.toString();
    }

    /**
     * Step 3：模拟 Full GC —— 创建持续持有引用的大对象，填满老年代。
     * <p>老年代满 → Full GC → 清理无法回收的对象 → 若仍不足 → OOM。
     * 对比短命对象（Eden 回收）和长命对象（老年代堆积）的差异。
     */
    public String simulateFullGC() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcCountBefore = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();

        // 创建持续引用的对象（不会被回收），逐渐填满老年代
        int mbEach = 10;
        int blockCount = 10;
        List<byte[]> longLived = new ArrayList<>();
        try {
            for (int i = 0; i < blockCount; i++) {
                longLived.add(new byte[mbEach * 1024 * 1024]); // 每个 10MB
            }
        } catch (OutOfMemoryError e) {
            // 堆不够了，说明已接近上限
        }

        System.gc();
        long gcCountAfter = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;

        StringBuilder sb = new StringBuilder();
        sb.append("=== 3. Full GC 模拟 ===\n");
        sb.append("创建 ").append(longLived.size()).append(" 个 ").append(mbEach).append("MB 持续引用对象 (共 ~")
                .append(longLived.size() * mbEach).append("MB)\n");
        sb.append("GC 次数变化: ").append(gcCountBefore).append(" → ").append(gcCountAfter).append('\n');
        sb.append("当前堆使用: ").append(heapUsed).append("MB\n");
        sb.append("这些对象有强引用，不会被 GC 回收，将持续驻留老年代直至方法结束\n");

        // 清理，避免影响后续演示
        longLived.clear();
        return sb.toString();
    }

    /**
     * Step 4：GC 日志解读指南（不实际打印，给出关键字段含义）。
     *
     * <p>开启 GC 日志的参数（JDK8）：
     * <pre>-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log</pre>
     *
     * <p>关键日志片段含义：
     * <pre>
     * [GC (Allocation Failure) [PSYoungGen: 65536K->10745K(76288K)] 65536K->10753K(251392K), 0.005s]
     *   → Young GC：分配失败触发，Eden 区 65536K → 10745K（容量 76288K），整堆 65536K → 10753K，耗时 5ms
     *
     * [Full GC (System.gc()) [PSYoungGen: 0K->0K(76288K)] [ParOldGen: 1024K->0K(175104K)] 1024K->0K(251392K), 0.01s]
     *   → Full GC：整堆回收，Parallel Scavenge(Young) + Parallel Old
     * </pre>
     */
    public String gcLogGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 4. GC 日志解读指南 ===\n\n");

        sb.append("【开启 GC 日志】(JDK8 参数):\n");
        sb.append("  -XX:+PrintGCDetails         打印 GC 详情\n");
        sb.append("  -XX:+PrintGCDateStamps      打印日期时间戳\n");
        sb.append("  -Xloggc:gc.log              输出到文件\n");
        sb.append("  -XX:+PrintHeapAtGC          GC 前后打印堆信息\n");
        sb.append("  -XX:+PrintTenuringDistribution 打印年龄分布\n\n");

        sb.append("【常用调优参数】:\n");
        sb.append("  -Xms2g -Xmx2g               堆初始/最大 2GB（建议设为相同值避免堆伸缩）\n");
        sb.append("  -Xmn512m                    新生代 512MB\n");
        sb.append("  -XX:SurvivorRatio=8         Eden:S0:S1 = 8:1:1\n");
        sb.append("  -XX:MaxTenuringThreshold=15 对象晋升老年代的年龄阈值\n");
        sb.append("  -XX:+UseG1GC                使用 G1 收集器\n");
        sb.append("  -XX:MaxGCPauseMillis=200    G1 最大暂停时间目标(ms)\n\n");

        sb.append("【GC 日志关键字段】:\n");
        sb.append("  Allocation Failure  → 分配失败（触因）\n");
        sb.append("  PSYoungGen          → Parallel Scavenge 年轻代\n");
        sb.append("  65536K->10745K      → 回收前后大小\n");
        sb.append("  (76288K)            → 该区域总容量\n");
        sb.append("  0.005s              → GC 耗时\n");

        return sb.toString();
    }

    /* ========== 统一入口 ========== */

    public static void main(String[] args) {
        GarbageCollectorDemo demo = new GarbageCollectorDemo();
        System.out.println(demo.detectCurrentGC());
        System.out.println(demo.simulateMinorGC());
        System.out.println(demo.simulateFullGC());
        System.out.println(demo.gcLogGuide());

        System.out.println(">>> 提示：使用以下 JVM 参数重新运行以观察 GC 行为 <<<");
        System.out.println("  java -Xms64m -Xmx64m -XX:+PrintGCDetails -XX:+PrintGCDateStamps GarbageCollectorDemo");
    }
}
