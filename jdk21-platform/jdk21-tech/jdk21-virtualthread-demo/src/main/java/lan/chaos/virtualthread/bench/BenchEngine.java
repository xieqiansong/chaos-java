package lan.chaos.virtualthread.bench;

import lan.chaos.virtualthread.common.constant.ExecutorMode;
import lan.chaos.virtualthread.common.model.BenchResult;
import lan.chaos.virtualthread.common.util.ConcurrentCounter;
import lan.chaos.virtualthread.common.util.LatencyRecorder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 压测引擎：按「闭环负载」模型把任务灌进指定执行器，采集吞吐、延迟分位与饱和拒绝三项指标。
 *
 * <p>WHY 闭环负载（concurrency 控制在途任务数，提交前取许可、完成后归还）：
 * 一次性全量提交时，平台线程池会把多出来的任务全塞进队列，测到的是「排队深度」而非稳定状态；
 * 闭环模型下服务端压力恒定在设定并发，测出来的吞吐与尾延迟才是稳态指标。
 *
 * <p>WHY 延迟记「提交 → 完成」而非「任务开始执行 → 结束」：
 * 前者包含排队等待，而排队正是线程池饱和最先崩的地方——只看执行耗时会把排队代价藏起来，
 * 得出「平台线程也很快」的错误结论。
 */
public final class BenchEngine {

    private BenchEngine() {
    }

    /** 压测任务体：index 为任务序号，异常由引擎统一计为失败。 */
    @FunctionalInterface
    public interface BenchTask {
        void execute(int index) throws Exception;
    }

    /** 便捷入口：引擎自建并关闭执行器。 */
    public static BenchResult run(String label, ExecutorMode mode, BenchOptions options, BenchTask task) {
        ExecutorService executor = ExecutorFactory.create(mode, options);
        try {
            return run(label, executor, options, task, new AtomicInteger());
        } finally {
            executor.shutdownNow();
        }
    }

    /** 核心入口：执行器由调用方管理（HTTP 场景需把执行器交给服务端，客户端另用一个）。 */
    public static BenchResult run(String label, ExecutorService executor, BenchOptions options, BenchTask task) {
        return run(label, executor, options, task, new AtomicInteger());
    }

    private static BenchResult run(String label, ExecutorService executor, BenchOptions options,
                                   BenchTask task, AtomicInteger rejected) {
        int taskCount = options.getTaskCount();
        warmup(executor, options, task);

        LatencyRecorder recorder = new LatencyRecorder(taskCount);
        ConcurrentCounter counter = new ConcurrentCounter();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        rejected.set(0);

        long startNanos = System.nanoTime();
        submitAll(executor, taskCount, options.getConcurrency(), task, recorder, counter, success, failed, rejected);
        long costMillis = (System.nanoTime() - startNanos) / 1_000_000L;

        LatencyRecorder.Stats stats = recorder.stats();
        return new BenchResult(label, taskCount, success.get(), failed.get(), rejected.get(),
                costMillis, counter.peak(),
                stats.avgMillis(), stats.p50Millis(), stats.p99Millis(), stats.maxMillis());
    }

    /** 预热：抵消 JIT 与线程创建的首次开销，结果一律丢弃。 */
    private static void warmup(ExecutorService executor, BenchOptions options, BenchTask task) {
        int warmupCount = options.warmupTaskCount();
        for (int round = 0; round < options.getWarmupRounds(); round++) {
            // 容量传 1：多余采样在 record 内被丢弃，避免预热阶段分配大数组
            submitAll(executor, warmupCount, options.getConcurrency(), task,
                    new LatencyRecorder(1), new ConcurrentCounter(),
                    new AtomicInteger(), new AtomicInteger(), new AtomicInteger());
        }
    }

    private static void submitAll(ExecutorService executor, int count, int concurrency, BenchTask task,
                                  LatencyRecorder recorder, ConcurrentCounter counter,
                                  AtomicInteger success, AtomicInteger failed, AtomicInteger rejected) {
        Semaphore inflight = new Semaphore(concurrency);
        CountDownLatch done = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            acquireQuietly(inflight);
            long submitNanos = System.nanoTime();
            int index = i;
            try {
                executor.execute(() -> {
                    try {
                        counter.enter();
                        task.execute(index);
                        success.incrementAndGet();
                    } catch (Throwable t) {
                        // 含饱和拒绝之外的业务异常：统一计失败，不让它中断整轮压测
                        failed.incrementAndGet();
                    } finally {
                        counter.exit();
                        recorder.record(System.nanoTime() - submitNanos);
                        done.countDown();
                        inflight.release();
                    }
                });
            } catch (RejectedExecutionException e) {
                rejected.incrementAndGet();
                done.countDown();
                inflight.release();
            }
        }
        awaitQuietly(done);
    }

    private static void acquireQuietly(Semaphore semaphore) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("压测提交被中断", e);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待压测任务完成被中断", e);
        }
    }
}
