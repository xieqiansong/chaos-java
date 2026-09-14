package lan.chaos.jdk17features.sealed;

/**
 * 密封接口（JEP 409，JDK17 定稿）：用 {@code permits} 显式枚举允许的实现类，
 * 再配合 permitted 类的 {@code final}/{@code sealed}/{@code non-sealed} 修饰，把"谁能继承我"写死在源码里。
 *
 * <p>WHY：以前父类无法限制子类范围，switch 一个接口时编译器无法判断分支是否穷尽。密封类让"可穷尽的模式匹配"成为可能。
 */
public sealed interface Shape permits Circle, Rectangle {
    double area();
}
