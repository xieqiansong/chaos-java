package lan.chaos.jdk17features.streamtolist;

import lan.chaos.jdk17features.common.SampleData;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Stream 收尾增强（JDK16）：{@code toList()} 与 {@code mapMulti()}。
 *
 * <p>WHY：
 * <ul>
 *   <li>旧写法 {@code collect(Collectors.toList())} 冗长且返回类型未声明"是否可变"；{@code Stream.toList()} 一行搞定，且返回<b>不可变</b>列表（避免被误改）；</li>
 *   <li>{@code mapMulti} 做"一对多"展开时无需先建中间集合（相比 {@code flatMap} 省一次中间 Stream 分配，且元素可条件性发射）。</li>
 * </ul>
 */
public class StreamToListDemo {

    public static void run() {
        List<Integer> evens = SampleData.sampleNumbers().stream()
                .filter(n -> n % 2 == 0)
                .toList();
        System.out.println("toList() 偶数: " + evens);

        // mapMulti：一对多展开（每个数发射自身与 ×10）
        List<Integer> expanded = SampleData.sampleNumbers().stream()
                .limit(3)
                .mapMulti((Integer n, Consumer<Integer> c) -> {
                    c.accept(n);
                    c.accept(n * 10);
                })
                .toList();
        System.out.println("mapMulti 展开: " + expanded);

        // toList 返回不可变列表
        try {
            evens.add(99);
        } catch (UnsupportedOperationException e) {
            System.out.println("toList() 返回不可变列表，add 抛: " + e.getClass().getSimpleName());
        }
    }
}
