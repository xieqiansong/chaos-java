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
    THREADLOCAL("ThreadLocal 语义：继承默认值与显式关闭"),

    // 压测量化层：机制演示回答「为什么」，压测回答「收益有多大、边界在哪」
    BENCH_IO("压测 A：IO 密集吞吐（延迟 × 并发 × 池大小）"),
    BENCH_SATURATION("压测 B：线程池饱和 → 排队 → 拒绝"),
    BENCH_PINNING("压测 C：synchronized pinning 的吞吐代价"),
    BENCH_CPU("压测 D：CPU 密集适用边界"),
    BENCH_HTTP("压测 E：HTTP 服务 平台线程 vs 虚拟线程");

    private final String desc;

    Scenario(String desc) {
        this.desc = desc;
    }

    public String desc() {
        return desc;
    }
}
