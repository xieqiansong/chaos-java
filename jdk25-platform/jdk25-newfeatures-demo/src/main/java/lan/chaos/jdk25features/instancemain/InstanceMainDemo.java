package lan.chaos.jdk25features.instancemain;

/**
 * 隐式声明类与实例 main 方法（JEP 483，JDK25 定稿）：启动类可以省略 {@code static}，连参数 {@code String[] args} 都可省，
 * 由 {@code java} 启动器自动实例化再调用。适合教学与极简入口。
 *
 * <p>WHY：初学者第一个 "Hello World" 要写 {@code public static void main(String[] args)} 一堆样板，
 * 该特性让入口更接近"普通方法"，降低门槛（底层仍是启动器实例化后调用）。本 demo 用实例方法 {@code main()} 演示。
 */
public class InstanceMainDemo {

    void main() {
        System.out.println("实例 main()：无需 static、无需 String[] args，java 启动器会自动实例化调用");
    }
}
