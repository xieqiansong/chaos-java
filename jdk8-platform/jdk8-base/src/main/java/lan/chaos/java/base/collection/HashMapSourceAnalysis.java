package lan.chaos.java.base.collection;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 能力一：HashMap 源码级分析（数据结构 / hash 算法 / put 流程 / resize 扩容）。
 *
 * <p>WHY：HashMap 是 Java 中使用频率最高的数据结构之一，面试必问，线上踩坑高频。
 * 理解其内部实现（数组+链表+红黑树、hash扰动、扩容机制）才能写出高性能代码，
 * 避免哈希碰撞退化、扩容雪崩等问题。
 *
 * <p>关键 API：put / get / resize / hash / tableSizeFor。
 * 生产坑：
 * <ul>
 *   <li>初始容量设太小 → 频繁扩容消耗 CPU</li>
 *   <li>key 的 hashCode() 分布不均 → 链表退化为 O(n)，JDK8 红黑树兜底（单链表 ≥8 / 数组 ≥64 才树化）</li>
 *   <li>多线程 put 会死循环/丢数据（JDK7 头插法死循环，JDK8 尾插法仍不安全）→ 用 ConcurrentHashMap</li>
 *   <li>loadFactor 默认 0.75 是时间空间折中，一般不需要改</li>
 * </ul>
 *
 * @see ConcurrentHashMapSourceAnalysis
 */
public class HashMapSourceAnalysis {

    /**
     * Step 1：理解 hash 扰动函数 —— 高 16 位与低 16 位异或，减少低位相同高位不同的碰撞。
     *
     * <p>HashMap 的 hash 值 = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)。
     * 原因：计算桶下标用的是 (n-1) & hash，当 n 较小时只有低位参与运算，
     * 高位异或到低位能让 hashCode 高位变化也影响桶下标。
     */
    public static int hash(Object key) {
        if (key == null) return 0;
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    /**
     * Step 2：理解 tableSizeFor —— 返回 ≥ cap 的最小 2 的幂。
     *
     * <p>HashMap 的容量永远是 2^n，这样 (n-1) & hash 等价于 hash % n 但更快。
     * 传入 10 返回 16，传入 17 返回 32。
     * 核心原理：把最高位 1 之后的所有位全部变成 1，然后 +1。
     */
    public static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= 1 << 30) ? 1 << 30 : n + 1;
    }

    /**
     * Step 3：put 流程演练 —— key → hash → 桶下标 → 头插/尾插/树插 → 检查扩容。
     *
     * <p>简化为三个场景演示：
     * <ol>
     *   <li>新 key 放入空槽</li>
     *   <li>同 hash 的 key 形成链表（哈希碰撞）</li>
     *   <li>容量达到阈值触发 resize（2 倍扩容 + rehash）</li>
     * </ol>
     */
    public String demonstratePutGet() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== HashMap put/get 流程演示 ===\n");

        Map<String, Integer> map = new HashMap<>(4); // 初始 4，阈值 = 4*0.75 = 3
        sb.append("初始容量=4, 阈值(threshold)=3\n");

        // 场景1：正常 put + get
        map.put("one", 1);
        sb.append("put('one',1)  -> size=").append(map.size()).append('\n');
        sb.append("get('one')   -> ").append(map.get("one")).append("  (O(1) 直接命中)\n");

        // 场景2：哈希碰撞（如果 key 的 hashCode 相同或桶下标相同）
        map.put("two", 2);
        sb.append("put('two',2)  -> size=").append(map.size()).append('\n');

        // 场景3：触发扩容（第4个元素会触发 resize，4 → 8）
        map.put("three", 3);
        sb.append("put('three',3) -> size=").append(map.size()).append("  (未达阈值)\n");

        map.put("four", 4);
        sb.append("put('four',4)  -> size=").append(map.size());
        sb.append("  (触发 resize: 4→8, rehash 重新分布已有元素)\n");

        sb.append("\n最终遍历：");
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append(' ');
        }
        return sb.toString();
    }

    /**
     * Step 4：通过反射窥探 HashMap 内部状态 —— table 长度、threshold、实际条目数。
     *
     * <p>演示「可观察」，帮助理解容量和实际元素数量的关系。
     */
    public String inspectInternal() throws Exception {
        Map<String, String> map = new HashMap<>(2);
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        Field sizeField = HashMap.class.getDeclaredField("size");
        sizeField.setAccessible(true);

        StringBuilder sb = new StringBuilder();
        sb.append("=== HashMap 内部状态窥探 ===\n");

        map.put("a", "A");
        Object[] table = (Object[]) tableField.get(map);
        int threshold = (int) thresholdField.get(map);
        int size = (int) sizeField.get(map);

        sb.append("put('a','A') → table.len=").append(table.length)
                .append(", threshold=").append(threshold)
                .append(", size=").append(size).append('\n');

        map.put("b", "B");
        table = (Object[]) tableField.get(map);
        threshold = (int) thresholdField.get(map);
        size = (int) sizeField.get(map);
        sb.append("put('b','B') → table.len=").append(table.length)
                .append(", threshold=").append(threshold)
                .append(", size=").append(size);
        sb.append("  (注意：初始 cap=2 给的是 4，因为 tableSizeFor(2)=4, threshold=3)\n");

        map.put("c", "C");
        map.put("d", "D"); // 超过 4*0.75=3，触发扩容
        table = (Object[]) tableField.get(map);
        threshold = (int) thresholdField.get(map);
        size = (int) sizeField.get(map);
        sb.append("put 至 size=4  → table.len=").append(table.length)
                .append(", threshold=").append(threshold)
                .append(", size=").append(size);
        sb.append("  (resize: 4→8)\n");

        return sb.toString();
    }

    /* ========== 统一入口 ========== */

    public static void main(String[] args) throws Exception {
        HashMapSourceAnalysis demo = new HashMapSourceAnalysis();

        System.out.println(">>> hash 扰动函数演示 <<<");
        System.out.println("  hash(\"hello\") = " + hash("hello"));
        System.out.println("  hash(null)      = " + hash(null));
        System.out.println();

        System.out.println(">>> tableSizeFor 容量取整 <<<");
        System.out.println("  tableSizeFor(1)  = " + tableSizeFor(1));
        System.out.println("  tableSizeFor(10) = " + tableSizeFor(10));
        System.out.println("  tableSizeFor(17) = " + tableSizeFor(17));
        System.out.println();

        System.out.println(demo.demonstratePutGet());
        System.out.println(demo.inspectInternal());
    }
}
