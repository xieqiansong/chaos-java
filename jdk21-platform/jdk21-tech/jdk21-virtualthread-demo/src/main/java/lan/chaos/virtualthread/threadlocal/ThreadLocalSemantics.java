package lan.chaos.virtualthread.threadlocal;

/**
 * WHY：虚拟线程默认继承父线程的可继承上下文（inheritInheritableThreadLocals 默认 true，与平台线程一致），
 * 但生产上不应依赖「线程本地变量隐式跨虚拟线程传递」——虚拟线程数量巨大、生命周期各异，
 * 隐式传递易导致上下文错乱与内存驻留；正确做法是显式传递（参数 / 结构化并发作用域）。
 * 演示：父线程设置 InheritableThreadLocal，子虚拟线程在默认与显式关闭继承两种配置下的读取结果。
 */
public class ThreadLocalSemantics {

    private static final InheritableThreadLocal<String> CONTEXT = new InheritableThreadLocal<>();

    /** 默认配置：子虚拟线程继承父线程的可继承上下文（与平台线程一致）。 */
    public String readInChildByDefault(String parentValue) throws Exception {
        CONTEXT.set(parentValue);
        try {
            String[] holder = new String[1];
            Thread child = Thread.ofVirtual().name("child-default").start(() -> holder[0] = CONTEXT.get());
            child.join();
            return holder[0];
        } finally {
            CONTEXT.remove();
        }
    }

    /** 显式关闭继承：inheritInheritableThreadLocals(false)，子虚拟线程读不到父线程的可继承上下文。 */
    public String readInChildWithInheritanceDisabled(String parentValue) throws Exception {
        CONTEXT.set(parentValue);
        try {
            String[] holder = new String[1];
            Thread child = Thread.ofVirtual().name("child-disabled")
                    .inheritInheritableThreadLocals(false)
                    .start(() -> holder[0] = CONTEXT.get());
            child.join();
            return holder[0];
        } finally {
            CONTEXT.remove();
        }
    }

    public void demo() {
        try {
            System.out.println("[输入] 父线程设置 InheritableThreadLocal=\"ctx-42\"，创建子虚拟线程读取");
            System.out.println("[输出] 默认配置：子虚拟线程读到 " + describe(readInChildByDefault("ctx-42")));
            System.out.println("[输出] 关闭继承：子虚拟线程读到 " + describe(readInChildWithInheritanceDisabled("ctx-42")));
        } catch (Exception e) {
            throw new IllegalStateException("ThreadLocal 继承语义演示失败", e);
        }
    }

    private static String describe(String value) {
        return value == null ? "null（未继承）" : "\"" + value + "\"（已继承）";
    }
}
