package lan.chaos.zookeeper.common.util;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;

/**
 * Curator 客户端工具：建立/检测与 ZooKeeper 的连接。
 *
 * <p>为什么用 Curator 而非原生 ZK 客户端：原生客户端 API 偏底层（Watcher 一次性、无重连），
 * Curator 封装了重连、重试、分布式原语（锁/选主/缓存），生产首选。
 */
public final class ZkClientUtil {

    private ZkClientUtil() {}

    /** 创建并启动一个 Curator 客户端。 */
    public static CuratorFramework newClient(String connectString) {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .sessionTimeoutMs(60_000)
                .build();
        client.start();
        return client;
    }

    /** 探测 ZK 是否可达（用于测试 Assumptions 优雅跳过）。 */
    public static boolean isAvailable(String connectString) {
        CuratorFramework client = newClient(connectString);
        try {
            return client.blockUntilConnected(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            client.close();
        }
    }
}
