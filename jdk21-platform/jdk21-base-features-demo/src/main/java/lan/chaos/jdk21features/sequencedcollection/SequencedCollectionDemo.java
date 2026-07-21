package lan.chaos.jdk21features.sequencedcollection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedSet;

/**
 * SequencedCollection（JEP 431，JDK21 定稿）：统一"有确定 encounter 顺序"的集合行为的新根接口。
 *
 * <p>WHY：以前要取列表首尾得用 {@code get(0)} / {@code get(size()-1)}，而 {@code LinkedHashSet} 没有首尾 API，
 * 逆序还得 {@code new ArrayList(set).reversed()} 之类绕弯。JDK21 让 {@code List}/{@code LinkedHashSet}/{@code LinkedHashMap} 等
 * 实现统一接口，提供 {@code getFirst()}/{@code getLast()}/{@code reversed()}（逆序视图，非拷贝）。
 * 包含 {@link SequencedSet}（{@code LinkedHashSet}）与 {@link SequencedCollection}（{@code ArrayList}）等子接口。
 */
public class SequencedCollectionDemo {

    public static void run() {
        SequencedCollection<String> seq = new ArrayList<>(List.of("a", "b", "c"));
        System.out.println("原序: " + seq);
        System.out.println("getFirst(): " + seq.getFirst());
        System.out.println("getLast(): " + seq.getLast());

        // reversed() 是视图，不修改原集合
        System.out.println("reversed() 视图: " + seq.reversed());
        System.out.println("原集合未被改动: " + seq);

        SequencedSet<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));
        System.out.println("SequencedSet 逆序视图: " + set.reversed());
    }
}
