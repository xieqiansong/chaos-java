package lan.chaos.jdk17features.sealed;

/**
 * 密封类演示：受限的实现层次 + 配合 instanceof 模式匹配做穷尽处理。
 *
 * <p>WHY 注释见 {@link Shape}。这里展示：因为实现类只有 Circle / Rectangle 两种，
 * 用 instanceof 模式匹配即可完整处理（编译器虽不强制 default，但人为保证穷尽）。
 */
public class SealedDemo {

    public static void run() {
        Shape circle = new Circle(2);
        System.out.println("Circle 面积: " + circle.area());
        System.out.println("describe(new Circle(2)): " + describe(new Circle(2)));
        System.out.println("describe(new Rectangle(3,4)): " + describe(new Rectangle(3, 4)));
    }

    static String describe(Shape s) {
        if (s instanceof Circle c) {
            return "圆形 r=" + c.r();
        }
        if (s instanceof Rectangle r) {
            return "矩形 " + r.w() + "x" + r.h();
        }
        // 枚举已穷尽，理论不会到达；保留以防将来扩展了 permitted 类
        throw new IllegalStateException("未知形状: " + s);
    }
}
