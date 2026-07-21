package lan.chaos.jdk8features.defaultmethod;

/**
 * 接口默认/静态方法（JDK8）：接口可包含带实现的方法，实现类无需改动即可获得新能力，
 * 解决了"给接口加方法必须改所有实现类"的演进难题（如 {@code Collection.removeIf} 就是这么加进来的）。
 *
 * <p>规则：
 * <ul>
 *   <li>实现类可直接继承默认方法，也可重写；</li>
 *   <li>静态方法属于接口本身（用 {@code Interface.staticMethod()} 调用）；</li>
 *   <li>多接口继承出现同名默认方法冲突时，实现类必须显式重写（用 {@code Interface.super.method()} 指定）。</li>
 * </ul>
 */
public interface Calculator {

    int add(int a, int b);

    /** 默认方法：实现类自动获得，无需各自实现 */
    default int multiply(int a, int b) {
        return a * b;
    }

    /** 静态方法：工具方法，属于接口 */
    static Calculator create() {
        return (a, b) -> a + b;
    }
}
