package lan.chaos.virtualthread.pinning;

import lan.chaos.virtualthread.common.model.LoadResult;
import lan.chaos.virtualthread.common.util.ConcurrentCounter;
import lan.chaos.virtualthread.common.util.IoSimulator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WHY：虚拟线程执行 synchronized 临界区时会被「钉住」(pinning)——即使临界区内阻塞，
 * 也不卸载、占住载体线程不放，导致虚拟线程退化成平台线程的并发能力（JDK 24 的 JEP 491 才修复）。
 * 每个任务持有自己的锁，临界区内做模拟 IO；对照 ReentrantLock（阻塞时正常卸载）。
 * 观察指标：临界区峰值并发（pinned 版被限制在载体线程数内）+ 总耗时。
 */
public class PinningCompare {

    private final int taskCount;
    private final long ioMillis;

    public PinningCompare() {
        this(400, 10);
    }

    public PinningCompare(int taskCount, long ioMillis) {
        this.taskCount = taskCount;
        this.ioMillis = ioMillis;
    }

    public record Comparison(LoadResult pinned, LoadResult unlocked, int carriers) {

        @Override
        public String toString() {
            return "载体线程数(近似CPU核数)=" + carriers + "\n"
                    + pinned.pretty() + "\n" + unlocked.pretty();
        }
    }

    public Comparison run() {
        LoadResult pinned = measurePinned();
        LoadResult unlocked = measureWithReentrantLock();
        return new Comparison(pinned, unlocked, Runtime.getRuntime().availableProcessors());
    }

    /** 对照组 A：synchronized 临界区内阻塞 → pin 住载体线程。 */
    private LoadResult measurePinned() {
        Object[] locks = new Object[taskCount];
        for (int i = 0; i < taskCount; i++) {
            locks[i] = new Object();
        }
        ConcurrentCounter counter = new ConcurrentCounter();
        CountDownLatch done = new CountDownLatch(taskCount);
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                Object lock = locks[i];
                pool.submit(() -> {
                    synchronized (lock) {
                        counter.enter();
                        try {
                            IoSimulator.ioBlock(ioMillis);
                        } finally {
                            counter.exit();
                            done.countDown();
                        }
                    }
                });
            }
        }
        awaitQuietly(done);
        long costMillis = (System.nanoTime() - start) / 1_000_000;
        return LoadResult.builder()
                .label("synchronized(临界区内阻塞)").taskCount(taskCount).ioMillis(ioMillis)
                .costMillis(costMillis).peakConcurrency(counter.peak()).build();
    }

    /** 对照组 B：ReentrantLock 临界区内阻塞 → 阻塞时正常卸载。 */
    private LoadResult measureWithReentrantLock() {
        ReentrantLock[] locks = new ReentrantLock[taskCount];
        for (int i = 0; i < taskCount; i++) {
            locks[i] = new ReentrantLock();
        }
        ConcurrentCounter counter = new ConcurrentCounter();
        CountDownLatch done = new CountDownLatch(taskCount);
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                ReentrantLock lock = locks[i];
                pool.submit(() -> {
                    lock.lock();
                    try {
                        counter.enter();
                        try {
                            IoSimulator.ioBlock(ioMillis);
                        } finally {
                            counter.exit();
                        }
                    } finally {
                        lock.unlock();
                        done.countDown();
                    }
                });
            }
        }
        awaitQuietly(done);
        long costMillis = (System.nanoTime() - start) / 1_000_000;
        return LoadResult.builder()
                .label("ReentrantLock(临界区内阻塞)").taskCount(taskCount).ioMillis(ioMillis)
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
        System.out.println("[输入] 任务数=" + taskCount + "，每个任务持有自己的锁，临界区内模拟IO " + ioMillis + "ms");
        System.out.println("[输出]");
        System.out.println("  " + run());
        System.out.println("  说明：pinned 版峰值并发≈载体线程数（临界区内阻塞不卸载），unlocked 版峰值并发可远超载体线程数。");
    }
}
