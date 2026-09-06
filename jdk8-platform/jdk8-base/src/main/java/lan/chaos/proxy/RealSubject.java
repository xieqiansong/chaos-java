package lan.chaos.proxy;

/**
 * 真实实现：同时作为 CGLib 的被代理类（CGLib 通过继承实现，不需要接口）。
 */
public class RealSubject implements Subject {

    @Override
    public String hello(String name) {
        return "Hello, " + name;
    }

    @Override
    @Measured("compute")
    public int compute(int a, int b) {
        // 故意睡 20ms，方便观察计时增强
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return a + b;
    }

    @Override
    @NoIntercept
    public void noop() {
        System.out.println("[RealSubject.noop] 真实执行，绕过代理拦截");
    }
}
