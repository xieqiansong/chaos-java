package lan.chaos.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * JDK 原生动态代理（基于接口）。
 *
 * <p>进阶点：
 * <ol>
 *   <li>对 {@code hashCode/equals/toString} 做特殊转发，保证代理对象身份语义与真实对象一致；</li>
 *   <li>从接口方法或真实实现方法上读取 {@link Measured} 注解，实现「注解驱动」的选择性增强；</li>
 *   <li>提供通用工厂方法 {@link #create(Object, Class)}，与具体业务解耦。</li>
 * </ol>
 */
public class JdkDynamicProxy implements InvocationHandler {

    private final Object target;

    private JdkDynamicProxy(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        // 1) 对象身份方法：避免代理后 hashCode/equals 行为错乱
        switch (name) {
            case "hashCode":
                return System.identityHashCode(target);
            case "equals":
                return proxy == args[0] || target.equals(args[0]);
            case "toString":
                return "JdkProxy(" + target + ")";
            default:
                // 继续往下走正常拦截逻辑
        }

        // 2) 读取注解：优先接口方法，其次真实实现方法（注解驱动增强）
        Measured measured = readAnnotation(method, Measured.class);

        long start = System.nanoTime();
        try {
            return method.invoke(target, args);
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            String tag = (measured != null && !measured.value().isEmpty())
                    ? measured.value()
                    : method.getName();
            System.out.printf("[JDK 拦截] %s -> %d ms%n", tag, costMs);
        }
    }

    /**
     * 先从接口方法取注解，取不到再回退到真实实现类的方法上取。
     */
    private <A extends Annotation> A readAnnotation(Method ifMethod, Class<A> type) {
        A ann = ifMethod.getAnnotation(type);
        if (ann != null) {
            return ann;
        }
        try {
            Method real = target.getClass().getMethod(ifMethod.getName(), ifMethod.getParameterTypes());
            return real.getAnnotation(type);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    /**
     * 通用工厂：返回一个实现了 {@code interfaceType} 的代理对象。
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(T target, Class<T> interfaceType) {
        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException("JDK 动态代理只能代理接口: " + interfaceType);
        }
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                new JdkDynamicProxy(target));
    }
}
