package lan.chaos.jdk21features.patternswitch;

/**
 * 模式匹配 switch（JEP 441，JDK21 定稿）：{@code case} 可直接写类型模式/常量，并绑定变量，替代一堆 {@code if/instanceof} 与旧 switch 转型。
 *
 * <p>WHY：过去对 {@code Object} 按类型分发要写一长串 {@code if (x instanceof T t)}；旧 switch 又只能匹配常量、且易漏 break。
 * 模式匹配 switch 让"按类型+提取值"在一个 switch 表达式里完成，且编译器能校验穷尽（配合 sealed 类型可省 default）。
 * 注意：JDK21 已正式支持 {@code case Integer i}、{@code case String s}，无需预览参数。
 */
public class PatternSwitchDemo {

    public static void run() {
        Object[] values = {42, "hello", 3.14, true, 'x'};
        for (Object v : values) {
            System.out.println("describe(" + v + ") = " + describe(v));
        }
    }

    static String describe(Object o) {
        return switch (o) {
            case Integer i -> "int " + i;
            case String s -> "string len=" + s.length();
            case Double d -> "double " + d;
            case Boolean b -> "bool " + b;
            default -> "other " + o.getClass().getSimpleName();
        };
    }
}
