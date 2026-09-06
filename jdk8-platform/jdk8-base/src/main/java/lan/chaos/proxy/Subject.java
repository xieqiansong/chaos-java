package lan.chaos.proxy;

/**
 * 业务接口：JDK 动态代理只能代理接口，这里作为 JDK 代理的目标类型。
 */
public interface Subject {

    String hello(String name);

    @Measured("compute")
    int compute(int a, int b);

    void noop();
}
