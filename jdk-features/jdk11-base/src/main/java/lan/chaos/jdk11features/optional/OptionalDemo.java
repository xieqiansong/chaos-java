package lan.chaos.jdk11features.optional;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Optional 增强（JDK9+）：把"有值/无值"的多种处理从外部 if 收进 Optional 自身。
 *
 * <p>关键 API / 规则：
 * <ul>
 *   <li>{@code ifPresentOrElse(action, emptyAction)} 有值/无值各执行一支；</li>
 *   <li>{@code or(supplier)} 为空时改投另一个 Optional；</li>
 *   <li>{@code stream()} 把 Optional 转成 0/1 个元素的 Stream，方便 flatMap 串联；</li>
 *   <li>{@code isEmpty()} / {@code orElseThrow()}（无参，JDK10 起）。</li>
 * </ul>
 */
public class OptionalDemo {

    public static void run() {
        Optional<String> o = Optional.of("value");
        o.ifPresentOrElse(v -> System.out.println("有值: " + v), () -> System.out.println("无值"));

        Optional<String> empty = Optional.empty();
        String r = empty.or(() -> Optional.of("fallback")).get();
        System.out.println("or 兜底: " + r);

        long count = Stream.of(Optional.of("a"), Optional.empty(), Optional.of("b"))
                .flatMap(Optional::stream)
                .count();
        System.out.println("stream 非空计数: " + count);

        System.out.println("Optional.empty().isEmpty(): " + Optional.empty().isEmpty());
    }
}
