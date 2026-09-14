package lan.chaos.jdk8features.lambda;

import lan.chaos.jdk8features.common.SampleData;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Lambda 表达式（JDK8 引入）：把"行为"当作参数传递，大幅替代匿名内部类。
 *
 * <p>WHY：此前要给集合排个序、加个过滤，得写一整块匿名内部类；Lambda 让"一小段逻辑"变成可传值的一等公民。
 * 关键 API / 规则：
 * <ul>
 *   <li>只有<b>函数式接口</b>（仅含一个抽象方法的接口）才能用 lambda 简写；</li>
 *   <li>{@code java.util.function} 提供 Predicate/Function/Consumer/Supplier 等通用函数式接口；</li>
 *   <li>lambda 可捕获 effectively final 的外部变量；</li>
 *   <li>变量类型推断由编译器完成，无需手写参数类型。</li>
 * </ul>
 */
public class LambdaDemo {

    public static void run() {
        List<Integer> nums = SampleData.sampleNumbers();
        System.out.println("原始: " + nums);

        // lambda 替代匿名内部类：偶数过滤
        Predicate<Integer> isEven = n -> n % 2 == 0;
        List<Integer> evens = nums.stream().filter(isEven).collect(Collectors.toList());
        System.out.println("偶数(filter + lambda): " + evens);

        // 行为参数化：把"大于5"这个判断直接内联成 lambda
        List<Integer> big = nums.stream().filter(n -> n > 5).collect(Collectors.toList());
        System.out.println("大于5(内联 lambda): " + big);

        // 传统写法对照（仅展示，不运行）：同样的逻辑在 JDK7 要写成匿名类
        // new Predicate<Integer>() { public boolean test(Integer n) { return n % 2 == 0; } }
    }
}
