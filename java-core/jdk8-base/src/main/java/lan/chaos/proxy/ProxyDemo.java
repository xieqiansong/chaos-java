package lan.chaos.proxy;

/**
 * 动态代理综合示例入口：对比 JDK 动态代理与 CGLib 动态代理的写法与进阶用法。
 *
 * <p>运行：直接执行 {@code main}，或
 * {@code mvn -pl jdk8-base exec:java -Dexec.mainClass=lan.chaos.proxy.ProxyDemo}
 */
public class ProxyDemo {

    public static void main(String[] args) {
        Subject real = new RealSubject();

        System.out.println("===== 1. JDK 动态代理（接口代理 + 注解驱动增强） =====");
        Subject jdk = JdkDynamicProxy.create(real, Subject.class);
        System.out.println("  hello -> " + jdk.hello("world"));
        System.out.println("  compute -> " + jdk.compute(3, 4));
        jdk.noop(); // JDK 代理对所有方法统一拦截（无路由能力）
        System.out.println("  toString -> " + jdk);

        System.out.println("\n===== 2. CGLib 动态代理（继承代理 + CallbackFilter 路由） =====");
        Subject cglib = CglibDynamicProxy.createProxy(real);
        System.out.println("  hello -> " + cglib.hello("world"));
        System.out.println("  compute -> " + cglib.compute(3, 4));
        cglib.noop(); // 被 @NoIntercept 标记，走 NoOp，不被拦截
        System.out.println("  toString -> " + cglib);

        System.out.println("\n===== 3. CGLib LazyLoader 懒加载 =====");
        // 昂贵初始化（sleep 模拟）放在 supplier 里，首次调用才执行
        HeavyService lazy = CglibDynamicProxy.createLazy(HeavyService.class, () -> {
            System.out.println("  [CGLib LazyLoader] 首次访问，执行昂贵初始化...");
            Thread.sleep(30);
            return new HeavyService();
        });
        System.out.println("  代理已创建（此时尚未做昂贵初始化）");
        System.out.println("  第一次 work() -> " + lazy.work());
        System.out.println("  第二次 work() -> " + lazy.work());
    }
}
