package lan.chaos.jdk11features.stream;

import lan.chaos.jdk11features.common.SampleData;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stream 增强（JDK9）：新增 takeWhile / dropWhile 与 ofNullable 等。
 *
 * <p>WHY：以前"取到条件不满足为止""跳过前缀"得自己写循环或借助第三方；{@code ofNullable} 省去 {@code s == null ? Stream.empty() : Stream.of(s)}。
 * 关键 API / 规则：
 * <ul>
 *   <li>{@code takeWhile(p)} 取前缀直到条件不成立（遇到第一个不满足即停）；</li>
 *   <li>{@code dropWhile(p)} 丢掉前缀直到条件不成立；</li>
 *   <li>{@code Stream.ofNullable(x)} 为 null 时返回空流。</li>
 * </ul>
 */
public class StreamDemo {

    public static void run() {
        List<Integer> nums = SampleData.sampleNumbers(); // 1..10

        List<Integer> taken = nums.stream().takeWhile(n -> n < 5).collect(Collectors.toList());
        System.out.println("takeWhile(<5): " + taken);

        List<Integer> dropped = nums.stream().dropWhile(n -> n < 5).collect(Collectors.toList());
        System.out.println("dropWhile(<5): " + dropped);

        Optional<Integer> one = Stream.ofNullable("7").map(Integer::valueOf).findFirst();
        System.out.println("ofNullable(非null) count: " + Stream.ofNullable("7").count());
        System.out.println("ofNullable(null) count: " + Stream.ofNullable(null).count());
    }
}
