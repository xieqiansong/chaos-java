package lan.chaos.virtualthread.common.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发运行数统计：enter()/exit() 围住待观察的临界区，
 * peak() 给出「同时运行峰值」——用于证明阻塞期间线程是否让出了调度器。
 * 平台线程池固定 N 个线程时峰值恒等于 N；虚拟线程阻塞即卸载，峰值远小于任务数。
 */
public final class ConcurrentCounter {

    private final AtomicInteger current = new AtomicInteger();
    private final AtomicInteger peak = new AtomicInteger();

    public void enter() {
        int now = current.incrementAndGet();
        peak.accumulateAndGet(now, Math::max);
    }

    public void exit() {
        current.decrementAndGet();
    }

    public int peak() {
        return peak.get();
    }
}
