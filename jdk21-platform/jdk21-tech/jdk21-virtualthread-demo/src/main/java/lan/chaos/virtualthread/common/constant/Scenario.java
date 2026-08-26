package lan.chaos.virtualthread.common.constant;

/**
 * 虚拟线程技术点拆解的五个演示场景：
 * THROUGHPUT —— 吞吐对比：IO 阻塞场景下平台线程池与虚拟线程的吞吐差异（为什么用）。
 * RUNTIME    —— 运行时调度：载体线程的挂载/卸载/复用观察（虚拟线程怎么跑）。
 * PINNING    —— pinning 复现：synchronized 临界区内阻塞导致载体线程被钉住（生产坑）。
 * STRUCTURED —— 结构化并发：StructuredTaskScope 并行/失败传播/超时（正确用法）。
 * THREADLOCAL—— ThreadLocal 继承语义：虚拟线程默认不继承父线程可继承上下文（使用边界）。
 */
public enum Scenario {
    THROUGHPUT("吞吐对比：平台线程池 vs 虚拟线程"),
    RUNTIME("运行时调度：载体线程挂载/卸载/复用"),
    PINNING("pinning 复现：synchronized vs ReentrantLock"),
    STRUCTURED("结构化并发：并行 / 失败传播 / 超时"),
    THREADLOCAL("ThreadLocal 语义：继承默认值与显式关闭");

    private final String desc;

    Scenario(String desc) {
        this.desc = desc;
    }

    public String desc() {
        return desc;
    }
}
