package lan.chaos.jdk11features.predicate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Predicate.not（JDK11）：更语义化地表达"取反"，替代 {@code x -> !predicate.test(x)}。
 *
 * <p>WHY：取反的 lambda 可读性差；{@code Predicate.not(p)} 把"非"提到调用点，意图清晰。
 * 常与 {@code Objects::nonNull} 配合过滤 null。
 */
public class PredicateDemo {

    public static void run() {
        List<String> list = Arrays.asList("a", "", "b", null, "c");

        List<String> withoutNulls = list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        System.out.println("去 null: " + withoutNulls);

        // 注意：先去 null，否则对 null 调用 String::isEmpty 会 NPE
        List<String> nonEmpty = list.stream()
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.toList());
        System.out.println("非空且非null: " + nonEmpty);
    }
}
