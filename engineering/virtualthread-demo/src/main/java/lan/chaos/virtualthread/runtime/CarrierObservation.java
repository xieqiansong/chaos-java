package lan.chaos.virtualthread.runtime;

import lan.chaos.virtualthread.common.util.IoSimulator;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WHY：虚拟线程的运行时表现为「挂载/卸载」——运行中的虚拟线程挂在一个载体线程(carrier)上，
 * 阻塞时从载体上卸下、载体转去服务别的虚拟线程。
 * 本类并发启动一批虚拟线程，统计两个卸载/复用证据：
 * 1) 总耗时：全部任务各自阻塞 ioMillis，若阻塞占用载体线程，总耗时≈任务数/载体数×ioMillis；
 *    卸载时总耗时≈单个任务的阻塞时长（载体被轮转复用）；
 * 2) 去重载体数：运行中虚拟线程 toString 的 @载体名 去重数量 ≈ 调度器并行度（默认=CPU 核数）。
 */
public class CarrierObservation {

    public static final int DEFAULT_VIRTUAL_COUNT = 128;
    public static final long DEFAULT_IO_MILLIS = 100;

    /** 观察结果：任务数 / 单任务IO / 总耗时 / 去重载体数。 */
    public record Observation(int virtualCount, long ioMillis, long costMillis, int uniqueCarriers) {

        @Override
        public String toString() {
            return "任务数=" + virtualCount + " 单任务IO=" + ioMillis + "ms 总耗时=" + costMillis
                    + "ms 去重载体线程数=" + uniqueCarriers;
        }
    }

    public Observation run(int virtualCount, long ioMillis) {
        Set<String> carriers = ConcurrentHashMap.newKeySet();
        CountDownLatch done = new CountDownLatch(virtualCount);
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < virtualCount; i++) {
                pool.submit(() -> {
                    try {
                        IoSimulator.ioBlock(ioMillis);
                        captureCarrier(carriers);
                    } finally {
                        done.countDown();
                    }
                });
            }
        }
        awaitQuietly(done);
        long costMillis = (System.nanoTime() - start) / 1_000_000;
        return new Observation(virtualCount, ioMillis, costMillis, carriers.size());
    }

    /** 从运行中的虚拟线程 toString 提取载体线程名（格式 .../runnable@ForkJoinPool-N-worker-M）。 */
    private static void captureCarrier(Set<String> carriers) {
        String text = Thread.currentThread().toString();
        int at = text.indexOf('@');
        if (at >= 0) {
            carriers.add(text.substring(at + 1));
        }
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
        System.out.println("[输入] 并发启动 " + DEFAULT_VIRTUAL_COUNT + " 个虚拟线程，各模拟IO " + DEFAULT_IO_MILLIS + "ms");
        printRunningSamples(3);
        Observation obs = run(DEFAULT_VIRTUAL_COUNT, DEFAULT_IO_MILLIS);
        int carriers = Runtime.getRuntime().availableProcessors();
        long serialLike = (long) Math.ceil(DEFAULT_VIRTUAL_COUNT / (double) carriers) * DEFAULT_IO_MILLIS;
        System.out.println("[输出] " + obs);
        System.out.println("  说明：总耗时(" + obs.costMillis() + "ms)≈单任务阻塞时长(" + DEFAULT_IO_MILLIS + "ms)，"
                + "远小于不卸载时的理论耗时(" + serialLike + "ms)；"
                + "载体线程去重数(" + obs.uniqueCarriers() + ")≈CPU核数(" + carriers + ") → 阻塞被卸载、载体被复用");
    }

    /** 打印少量运行中虚拟线程的 toString 样例，展示运行态携带载体线程名。 */
    private static void printRunningSamples(int count) {
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch done = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            pool.submit(() -> {
                IoSimulator.ioBlock(50);
                System.out.println("  VirtualThread toString = " + Thread.currentThread());
                done.countDown();
            });
        }
        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待样例输出被中断", e);
        }
        pool.close();
    }
}
