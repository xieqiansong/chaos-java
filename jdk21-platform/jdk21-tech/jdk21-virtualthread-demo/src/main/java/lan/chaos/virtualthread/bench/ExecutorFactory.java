package lan.chaos.virtualthread.bench;

import lan.chaos.virtualthread.common.constant.ExecutorMode;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 按模式创建执行器，并把「被拒绝的任务数」计数透出给压测引擎。
 *
 * <p>WHY 用有界队列 + AbortPolicy：无界队列下线程池饱和的表现是「任务无限排队、延迟无限拉长」，
 * 生产上通常配的是有界队列 + 拒绝策略，因此压测要复现的是「排队到拒绝」的真实失效链路。
 * CallerRunsPolicy 会让提交线程自己去跑任务，等于把压力倒灌回压测端，无法反映服务端真实表现，故不用。
 */
public final class ExecutorFactory {

    private ExecutorFactory() {
    }

    public static ExecutorService create(ExecutorMode mode, BenchOptions options) {
        return mode == ExecutorMode.VIRTUAL ? createVirtual() : createPlatform(options);
    }

    /**
     * 平台线程池：固定线程数 + 有界队列，饱和即拒绝。
     * 拒绝只在这里抛出、由 {@link BenchEngine} 统一计数——工厂不计数，
     * 否则「抛异常处 +1、捕获处 +1」会把拒绝数算成两倍。
     */
    private static ExecutorService createPlatform(BenchOptions options) {
        int n = options.getPlatformThreads();
        ThreadFactory factory = new NamedThreadFactory("bench-platform");
        return new ThreadPoolExecutor(n, n, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(options.getQueueCapacity()),
                factory,
                (task, executor) -> {
                    throw new RejectedExecutionException("队列已满，任务被拒绝");
                });
    }

    /**
     * 虚拟线程：每任务一个虚拟线程。底层是无界的 ForkJoinPool 调度器，不存在队列饱和，
     * 因此拒绝数恒为 0——这正是与平台线程池的本质差异之一。
     */
    private static ExecutorService createVirtual() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /** 具名线程工厂：线程转储时一眼区分被测线程池与压测客户端线程。 */
    private record NamedThreadFactory(String prefix) implements ThreadFactory {

        private static final AtomicInteger SEQ = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + SEQ.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
