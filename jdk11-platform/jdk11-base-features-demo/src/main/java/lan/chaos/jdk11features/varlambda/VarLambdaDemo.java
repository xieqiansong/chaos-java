package lan.chaos.jdk11features.varlambda;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * var 用于 lambda 参数（JDK11）：lambda 参数可用 {@code var}，从而能加注解或让类型显式。
 *
 * <p>WHY：JDK10 引入 {@code var}（局部变量类型推断）；JDK11 允许在 lambda 参数上使用 {@code var}，
 * 好处是能给参数加注解（如 {@code @Nullable}），或在需要显式类型时写出来。
 * 注意：要么全用 var，要么全用省略类型，不能混用（如 {@code (var x, y) -> ...} 非法）。
 */
public class VarLambdaDemo {

    public static void run() {
        List<String> list = Arrays.asList("x", "yy", "zzz");

        // var 参数 + 注解（演示形态；这里用 @Deprecated 仅为说明可加注解）
        list.forEach((@Deprecated var s) -> System.out.println(s + " 长度=" + s.length()));

        // 用 var 让类型显式
        Predicate<String> isEmpty = (var s) -> s.isEmpty();
        Function<String, Integer> len = (var s) -> s.length();
        System.out.println("isEmpty(''): " + isEmpty.test(""));
        System.out.println("len('hello'): " + len.apply("hello"));
    }
}
