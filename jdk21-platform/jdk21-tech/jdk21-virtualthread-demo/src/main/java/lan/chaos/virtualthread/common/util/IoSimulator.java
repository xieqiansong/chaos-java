package lan.chaos.virtualthread.common.util;

/**
 * 模拟一次 IO 阻塞（远程调用 / 数据库等待）。
 * 用 sleep 代替真实网络等待：对虚拟线程调度机制而言，sleep 与阻塞 IO 同样会触发挂载/卸载，
 * 可作为 IO 密集负载的最小可复现替身；保持中断标志以遵循线程协作中断约定。
 */
public final class IoSimulator {

    private IoSimulator() {
    }

    public static void ioBlock(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模拟 IO 阻塞被中断", e);
        }
    }
}
