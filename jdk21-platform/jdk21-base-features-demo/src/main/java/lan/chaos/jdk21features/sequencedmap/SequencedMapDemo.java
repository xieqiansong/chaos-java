package lan.chaos.jdk21features.sequencedmap;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

/**
 * SequencedMap（JEP 431，JDK21 定稿）：统一的"有序映射"接口（{@code LinkedHashMap} 直接实现）。
 *
 * <p>WHY：以前 {@code LinkedHashMap} 的首尾 entry、逆序遍历没有标准 API；现在 {@link SequencedMap} 提供
 * {@code firstEntry()}/{@code lastEntry()} 以及 {@code putFirst()}/{@code putLast()}，并可 {@code reversed()} 逆序视图。
 */
public class SequencedMapDemo {

    public static void run() {
        SequencedMap<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        System.out.println("原序: " + map);
        System.out.println("firstEntry(): " + map.firstEntry());
        System.out.println("lastEntry(): " + map.lastEntry());
        System.out.println("reversed() 视图: " + map.reversed());

        // 头插 / 尾插
        map.putFirst("zero", 0);
        map.putLast("four", 4);
        System.out.println("putFirst/putLast 后: " + map);
    }
}
