package lan.chaos.proxy;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * CGLib 动态代理（基于继承 / ASM 字节码生成）。
 *
 * <p>进阶点：
 * <ol>
 *   <li>使用 {@link CallbackFilter} 把不同方法路由到不同回调：
 *       {@link Measured} 方法进入日志拦截器，{@link NoIntercept} 方法直接走 {@link NoOp} 绕过拦截；</li>
 *   <li>拦截器内用 {@link MethodProxy#invokeSuper} 调用父类方法（比反射更快，是 CGLib 推荐写法）；</li>
 *   <li>额外提供 {@link #createLazy(Class, Callable)} 演示 {@link LazyLoader}：
 *       真实对象首次被访问时才创建（典型用于延迟初始化昂贵资源）。</li>
 * </ol>
 */
public class CglibDynamicProxy {

    /**
     * 创建带「注解路由」的 CGLib 代理（继承 target 的真实类型）。
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target) {
        // 捕获真实目标类型，用来按方法名+参数定位「带注解的真实方法」
        final Class<?> superClass = target.getClass();

        MethodInterceptor logging = new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                Measured measured = readMeasured(superClass, method);
                long start = System.nanoTime();
                try {
                    // CGLib 推荐用 invokeSuper 调用父类实现，而非反射 method.invoke
                    return proxy.invokeSuper(obj, args);
                } finally {
                    long costMs = (System.nanoTime() - start) / 1_000_000;
                    String tag = (measured != null && !measured.value().isEmpty())
                            ? measured.value()
                            : method.getName();
                    System.out.printf("[CGLib 拦截] %s -> %d ms%n", tag, costMs);
                }
            }
        };

        // 路由：被 @NoIntercept 标记的方法返回 1（走 NoOp），其余返回 0（走日志拦截）
        CallbackFilter filter = new CallbackFilter() {
            @Override
            public int accept(Method method) {
                try {
                    Method real = superClass.getMethod(method.getName(), method.getParameterTypes());
                    if (real.isAnnotationPresent(NoIntercept.class)) {
                        return 1;
                    }
                } catch (NoSuchMethodException ignored) {
                    // 回退到默认拦截
                }
                return 0;
            }
        };

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(superClass);
        enhancer.setCallbacks(new Callback[]{logging, NoOp.INSTANCE});
        enhancer.setCallbackFilter(filter);
        return (T) enhancer.create();
    }

    /**
     * 创建懒加载代理：真实对象在首次方法调用时才由 supplier 构造（LazyLoader）。
     */
    @SuppressWarnings("unchecked")
    public static <T> T createLazy(Class<T> type, Callable<? extends T> supplier) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(type);
        enhancer.setCallback(new LazyLoader() {
            @Override
            public Object loadObject() throws Exception {
                return supplier.call();
            }
        });
        return (T) enhancer.create();
    }

    private static Measured readMeasured(Class<?> superClass, Method method) {
        Measured m = method.getAnnotation(Measured.class);
        if (m == null) {
            try {
                Method real = superClass.getMethod(method.getName(), method.getParameterTypes());
                m = real.getAnnotation(Measured.class);
            } catch (NoSuchMethodException ignored) {
                // 没有注解，按普通方法处理
            }
        }
        return m;
    }
}
