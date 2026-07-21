package lan.chaos.jdk25features.primitivepattern;

/**
 * 原始类型模式 Primitive Types in Patterns（JEP 488，JDK25 定稿）：{@code instanceof} 与 {@code switch} 的模式现在支持原始类型。
 *
 * <p>WHY：过去模式只能是引用类型（{@code case Integer i}），无法直接用原始类型模式（{@code case int i}）。
 * JDK25 起可在 switch/instanceof 中写原始类型模式，对装箱值自动拆箱匹配，使"按数值类型分发"更自然、更贴近底层。
 * 注意：原始类型模式对引用选择器会做兼容匹配（{@code Integer} 匹配 {@code case int i} 并拆箱）。
 */
public class PrimitivePatternDemo {

    public static void run() {
        Object[] values = {Integer.valueOf(5), Long.valueOf(7L), Double.valueOf(2.5), "text", true};
        for (Object v : values) {
            System.out.println("classify(" + v + ") = " + classify(v));
        }
    }

    static String classify(Object o) {
        return switch (o) {
            case int i -> "primitive int = " + i;
            case long l -> "primitive long = " + l;
            case double d -> "primitive double = " + d;
            case String s -> "string len=" + s.length();
            default -> "other " + o.getClass().getSimpleName();
        };
    }
}
