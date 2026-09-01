package lan.chaos.virtualthread.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 负载模拟：用可控的 CPU/阻塞行为替代真实外部调用，使压测可复现、可对比。
 *
 * <p>WHY：真实远程调用受网络抖动、服务端容量影响，同一组参数两次跑出来不可比；
 * 用 sleep 模拟等待时，对虚拟线程调度而言 sleep 与阻塞 IO 同样触发挂载/卸载，
 * 是 IO 密集负载的最小可复现替身。区分三种负载形态：
 * <ul>
 *   <li>{@link #ioBlock}      —— 纯等待（远程调用 / DB 往返），不占 CPU。</li>
 *   <li>{@link #jitterBlock}  —— 带抖动的等待，更接近真实下游耗时分布。</li>
 *   <li>{@link #syncBlock}    —— synchronized 临界区内的等待，用于复现 pinning。</li>
 *   <li>{@link #cpuBurn}      —— 忙等计算，用于验证「虚拟线程不擅长 CPU 密集」的边界。</li>
 * </ul>
 * 保持中断标志以遵循线程协作中断约定。
 */
public final class IoSimulator {

    private IoSimulator() {
    }

    /** 模拟一次 IO 阻塞（远程调用 / 数据库等待）。 */
    public static void ioBlock(long millis) {
        sleepQuietly(millis);
    }

    /** 带抖动的阻塞：[base, base+jitter) 毫秒，贴合真实下游耗时分布。 */
    public static void jitterBlock(long baseMillis, long jitterMillis) {
        long extra = jitterMillis <= 0 ? 0 : ThreadLocalRandom.current().nextLong(jitterMillis);
        sleepQuietly(baseMillis + extra);
    }

    /** synchronized 临界区内阻塞：JDK 21 下会 pin 住载体线程，临界区内阻塞不卸载。 */
    public static void syncBlock(Object lock, long millis) {
        synchronized (lock) {
            sleepQuietly(millis);
        }
    }

    /**
     * CPU 密集忙等：在给定时长内持续计算，占满一个 OS 线程。
     * 返回值参与计算链，避免 JIT 把无副作用循环整个消除掉。
     */
    public static long cpuBurn(long millis) {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        long acc = 0;
        while (System.nanoTime() < deadline) {
            acc += acc * 31 + 17;
        }
        return acc;
    }

    private static void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模拟 IO 阻塞被中断", e);
        }
    }
}
