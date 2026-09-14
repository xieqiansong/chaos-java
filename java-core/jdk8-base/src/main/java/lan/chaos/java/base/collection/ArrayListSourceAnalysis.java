package lan.chaos.java.base.collection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 能力三：ArrayList 动态数组（扩容机制 / RandomAccess / 与 LinkedList 对比）。
 *
 * <p>WHY：ArrayList 是最常用的 List 实现，底层是 Object[] 数组。
 * 理解它的扩容机制（1.5 倍增长 + Arrays.copyOf）能帮助预估内存、避免频繁扩容。
 * 另外要理解 RandomAccess 标记接口的意义 —— 决定 for-i 还是 iterator 遍历更快。
 *
 * <p>关键 API：add / get / remove / trimToSize / ensureCapacity。
 * 生产坑：
 * <ul>
 *   <li>能预估容量时用 new ArrayList<>(capacity) 设初始容量，避免 10→15→22→33 的反复扩容</li>
 *   <li>随机访问用 ArrayList（O(1)），频繁头插/头删用 LinkedList（O(1) vs O(n) 移动）</li>
 *   <li>subList() 返回的是视图，对 subList 的修改会影响原 list，且原 list 结构变化后 subList 会 ConcurrentModificationException</li>
 *   <li>Arrays.asList() 返回的 List 不能 add/remove（固定大小），但可以 set</li>
 *   <li>toArray(new T[0]) 比 toArray(new T[size]) 更快（JIT 优化）</li>
 * </ul>
 */
public class ArrayListSourceAnalysis {

    /**
     * 通过反射窥探 ArrayList 内部 elementData 数组长度（capacity）的变化。
     * 演示从默认容量 10 开始，逐步添加元素，观察何时触发扩容。
     */
    public String demonstrateExpansion() throws Exception {
        Field elementDataField = ArrayList.class.getDeclaredField("elementData");
        elementDataField.setAccessible(true);

        ArrayList<Integer> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("=== ArrayList 扩容机制演示 ===\n");
        sb.append("初始化 new ArrayList<>() → capacity=")
                .append(((Object[]) elementDataField.get(list)).length).append(" (空列表时默认空数组)\n");

        // 添加第一个元素时扩容到 10（或按需）
        list.add(1);
        sb.append("add 第 1 个 → capacity=")
                .append(((Object[]) elementDataField.get(list)).length).append(", size=").append(list.size()).append('\n');

        // 继续添加，观察何时扩容
        int lastCapacity = ((Object[]) elementDataField.get(list)).length;
        for (int i = 2; i <= 50; i++) {
            list.add(i);
            int curCapacity = ((Object[]) elementDataField.get(list)).length;
            if (curCapacity != lastCapacity) {
                sb.append("add 第 ").append(i).append(" 个 → capacity=")
                        .append(curCapacity).append(", size=").append(list.size())
                        .append("  ← 触发扩容! (旧=").append(lastCapacity)
                        .append(" → 新=").append(curCapacity)
                        .append(", 增长 ~").append(String.format("%.0f%%", (double) curCapacity / lastCapacity * 100 - 100))
                        .append(")\n");
                lastCapacity = curCapacity;
            }
        }
        sb.append("\n规律：ArrayList 扩容 = oldCapacity + (oldCapacity >> 1)，即 1.5 倍增长\n");
        return sb.toString();
    }

    /**
     * ArrayList vs LinkedList 性能对比：随机访问 vs 头插入。
     * ArrayList 实现 RandomAccess 接口，for-i 遍历 O(n)；LinkedList 头插 O(1)。
     */
    public String arrayListVsLinkedList() {
        int size = 100_000;
        StringBuilder sb = new StringBuilder();
        sb.append("=== ArrayList vs LinkedList (数据量=").append(size).append(") ===\n");

        // 随机访问（get）
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        long t1 = System.nanoTime();
        for (int i = 0; i < size; i++) arrayList.get(i);
        long t2 = System.nanoTime();
        for (int i = 0; i < size; i++) linkedList.get(i);
        long t3 = System.nanoTime();

        sb.append("随机访问(get): ArrayList=").append(String.format("%.1fms", (t2 - t1) / 1_000_000.0))
                .append(", LinkedList=").append(String.format("%.1fms", (t3 - t2) / 1_000_000.0))
                .append("  (ArrayList O(1) vs LinkedList O(n))\n");

        // 头插入
        t1 = System.nanoTime();
        for (int i = 0; i < 10_000; i++) arrayList.add(0, i);
        t2 = System.nanoTime();
        for (int i = 0; i < 10_000; i++) linkedList.add(0, i);
        t3 = System.nanoTime();

        sb.append("头插入(10k):  ArrayList=").append(String.format("%.1fms", (t2 - t1) / 1_000_000.0))
                .append(", LinkedList=").append(String.format("%.1fms", (t3 - t2) / 1_000_000.0))
                .append("  (LinkedList O(1) vs ArrayList O(n) 搬移)\n");

        return sb.toString();
    }

    /* ========== 统一入口 ========== */

    public static void main(String[] args) throws Exception {
        ArrayListSourceAnalysis demo = new ArrayListSourceAnalysis();
        System.out.println(demo.demonstrateExpansion());
        System.out.println(demo.arrayListVsLinkedList());
    }
}
