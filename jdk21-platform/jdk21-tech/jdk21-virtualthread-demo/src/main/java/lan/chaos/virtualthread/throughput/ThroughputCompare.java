package lan.chaos.virtualthread.throughput;

import lan.chaos.virtualthread.common.model.LoadResult;
import lan.chaos.virtualthread.common.util.ConcurrentCounter;
import lan.chaos.virtualthread.common.util.IoSimulator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WHY：IO 密集请求模型下，平台线程池以「固定线程数」承载任务，阻塞会占满线程、
 * 排队加剧；虚拟线程由 JVM 调度，阻塞时自动卸载让出载体线程，可近乎无上限地承载阻塞任务。
 * 本类用同一批 IO 阻塞任务分别跑两种执行器，输出总耗时/吞吐/峰值并发，量化差异。
 */
public class ThroughputCompare {

    private final int taskCount;
    private final long ioMillis;
    private final int platformThreads;

    public ThroughputCompare() {
        this(2000, 5, 16);
    }

    public ThroughputCompare(int taskCount, long ioMillis, int platformThreads) {
        this.taskCount = taskCount;
        this.ioMillis = ioMillis;
        this.platformThreads = platformThreads;
    }

    /** 平台线程池：固定 platformThreads 个线程，IO 阻塞时线程被占住。 */
    public LoadResult runOnFixedPool() {
        try (ExecutorService pool = Executors.newFixedThreadPool(platformThreads)) {
            return measure(pool, "FixedThreadPool(" + platformThreads + ")");
        }
    }

    /** 虚拟线程：每个任务一个虚拟线程，阻塞时自动卸载。 */
    public LoadResult runOnVirtualThreads() {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            return measure(pool, "VirtualThread");
        }
    }

    private LoadResult measure(ExecutorService pool, String label) {
        ConcurrentCounter counter = new ConcurrentCounter();
        CountDownLatch done = new CountDownLatch(taskCount);
        long start = System.nanoTime();
        for (int i = 0; i < taskCount; i++) {
            pool.submit(() -> {
                counter.enter();
                try {
                    IoSimulator.ioBlock(ioMillis);
                } finally {
                    counter.exit();
                    done.countDown();
                }
            });
        }
        awaitQuietly(done);
        long costMillis = (System.nanoTime() - start) / 1_000_000;
        return LoadResult.builder()
                .label(label).taskCount(taskCount).ioMillis(ioMillis)
                .costMillis(costMillis).peakConcurrency(counter.peak()).build();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待任务完成被中断", e);
        }
    }

    public void demo() {
        System.out.println("[输入] 任务数=" + taskCount + "，单任务模拟IO=" + ioMillis + "ms，平台线程池线程数=" + platformThreads);
        LoadResult fixed = runOnFixedPool();
        LoadResult vt = runOnVirtualThreads();
        System.out.println("[输出]");
        System.out.println(fixed.pretty());
        System.out.println(vt.pretty());
        System.out.printf("  吞吐倍数(虚拟线程/平台线程) = %.1f 倍%n",
                fixed.throughputPerSec() == 0 ? 0 : vt.throughputPerSec() / (double) fixed.throughputPerSec());
    }
}
