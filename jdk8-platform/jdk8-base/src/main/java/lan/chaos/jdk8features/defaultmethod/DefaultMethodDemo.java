package lan.chaos.jdk8features.defaultmethod;

/**
 * 演示 {@link Calculator} 的默认方法 / 静态方法 / 重写默认方法。
 */
public class DefaultMethodDemo {

    public static void run() {
        Calculator calc = Calculator.create();
        System.out.println("add(3,4)=" + calc.add(3, 4));
        // 直接享用接口默认实现，无需自己写
        System.out.println("默认方法 multiply(3,4)=" + calc.multiply(3, 4));

        // 实现类可重写默认方法
        Calculator overridden = new Calculator() {
            @Override
            public int add(int a, int b) {
                return a + b;
            }

            @Override
            public int multiply(int a, int b) {
                return (a * b) * 2; // 重写默认实现
            }
        };
        System.out.println("重写后 multiply(3,4)=" + overridden.multiply(3, 4));
    }
}
