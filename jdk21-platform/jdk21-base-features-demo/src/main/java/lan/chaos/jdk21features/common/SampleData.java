package lan.chaos.jdk21features.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 共享样例数据工厂（JDK21 可用 {@code List/Map/Set.of} 一行创建不可变集合）。
 */
public final class SampleData {

    private SampleData() {
    }

    public static List<Integer> sampleNumbers() {
        return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    public static List<String> sampleStrings() {
        return List.of("apple", "banana", "cherry", "date");
    }

    public static Map<String, Integer> sampleMap() {
        return Map.of("one", 1, "two", 2, "three", 3);
    }

    public static Set<String> sampleSet() {
        return Set.of("x", "y", "z");
    }
}
