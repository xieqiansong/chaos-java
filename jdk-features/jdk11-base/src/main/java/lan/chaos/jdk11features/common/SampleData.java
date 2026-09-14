package lan.chaos.jdk11features.common;

import java.util.List;

/**
 * 共享样例数据工厂（JDK11 可用 {@code List.of} 一行创建不可变集合）。
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
}
