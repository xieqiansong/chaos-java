package lan.chaos.jdk25features.flexiblector;

/**
 * 灵活构造器体 Flexible Constructor Bodies（JEP 513，JDK25 定稿）：构造器中可以在显式 {@code super()} / {@code this()} 调用之前
 * 书写语句（赋值、校验、局部变量计算等），只要不在此之前访问"正在构造的实例"（即 {@code this}、字段、方法）。
 *
 * <p>WHY：旧规则强制构造器首条语句必须是 {@code super()} 或 {@code this()}，导致"先校验入参、再调父类构造"这类需求
 * 只能把校验塞进父类的构造器、或改用工厂方法。灵活构造器体允许在 {@code super()} 之前做校验，让校验逻辑就近、可读性更好，
 * 也更早失败（fail-fast）。注意：{@code super()}/{@code this()} 之前仍不允许触碰实例成员，否则编译报错。
 *
 * <p>本 demo 用 {@link Positive}（继承自 {@link Base}）演示：在调用 {@code super(raw)} 之前先校验负数，
 * 这是旧规则下无法在构造器中直接表达的典型场景。
 */
public class FlexibleConstructorDemo {

    /** 基类：构造时接收已校验的正数。 */
    static class Base {
        final int v;

        Base(int v) {
            this.v = v;
        }
    }

    /** 只接受非负数的包装类，负数在 super() 前就被拒绝。 */
    public static class Positive extends Base {

        Positive(int raw) {
            // 灵活构造器体：在 super() 之前写校验语句，JDK25 前这是非法的
            if (raw < 0) {
                throw new IllegalArgumentException("必须为非负数，收到: " + raw);
            }
            super(raw); // 现在 super() 可以出现在语句之后
        }

        int squared() {
            return v * v;
        }
    }

    public static void run() {
        Positive ok = new Positive(5);
        System.out.println("new Positive(5).squared() = " + ok.squared());

        try {
            new Positive(-1);
        } catch (IllegalArgumentException e) {
            System.out.println("new Positive(-1) -> 抛出 " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
