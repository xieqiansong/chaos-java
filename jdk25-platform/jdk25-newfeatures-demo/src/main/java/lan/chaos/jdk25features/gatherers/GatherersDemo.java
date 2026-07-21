package lan.chaos.jdk25features.gatherers;

import lan.chaos.jdk25features.common.SampleData;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Stream Gatherers（JEP 461，JDK24 定稿）：在流里插入"自定义中间操作"，弥补 {@code map/filter/flatMap} 表达力不足的场景。
 *
 * <p>WHY：流的内置操作有限，遇到"滑动窗口、去重计数、一对多且依赖状态"等需求往往要退回集合或 {@code flatMap} 变形。
 * Gatherer 把中间操作抽象成可复用组件：{@code Stream.gather(Gatherer)}。内置 {@code Gatherers.windowFixed(n)} 做定长窗口；
 * 也可用 {@code Gatherer.of(...)} 自定义（下面演示"相邻元素差异"）。</p>
 */
public class GatherersDemo {

    public static void run() {
        // 内置：定长窗口（每 3 个一组，最后不足 3 个也成一组）
        List<List<Integer>> windows = SampleData.sampleNumbers().stream()
                .gather(Gatherers.windowFixed(3))
                .toList();
        System.out.println("windowFixed(3): " + windows);

        // 自定义：相邻元素差值（依赖前一个元素的"状态"）
        List<Integer> deltas = Stream.of(1, 4, 2, 7)
                .gather(adjacentDelta())
                .toList();
        System.out.println("相邻差值: " + deltas);
    }

    /** 自定义 Gatherer：把 (a,b) -> b-a 的差值依次发射，首元素无前驱则跳过。 */
    static Gatherer<Integer, ?, Integer> adjacentDelta() {
        return Gatherer.of(
                AtomicReference<Integer>::new,                       // 状态：上一个元素
                (AtomicReference<Integer> state, Integer element, Gatherer.Downstream<? super Integer> downstream) -> {
                    Integer prev = state.get();
                    if (prev != null) {
                        downstream.push(element - prev);
                    }
                    state.set(element);
                    return true;
                }
        );
    }
}
