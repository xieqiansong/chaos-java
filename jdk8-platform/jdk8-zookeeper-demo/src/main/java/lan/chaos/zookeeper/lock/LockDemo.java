package lan.chaos.zookeeper.lock;

import lan.chaos.zookeeper.common.constant.ZkConstant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * ★★★ 高频：ZK 分布式锁 —— 多进程/多实例争抢同一把锁，保证临界区互斥。
 *
 * <p>痛点：单机锁（synchronized/ReentrantLock）只管一个 JVM；分布式部署下要用外部协调者。
 * ZK 用「临时顺序节点」实现公平互斥锁：抢不到就监听前驱节点，前驱释放再抢。
 *
 * <p>关键 API：Curator 的 {@link InterProcessMutex#acquire} / {@link InterProcessMutex#release}。
 *
 * <p>生产坑：
 * <ul>
 *   <li>必须 {@code release}，否则客户端宕机靠「临时节点」自动释放（session 断开即删）。</li>
 *   <li>锁粒度要小；长时间持锁会阻塞其他实例。</li>
 *   <li>羊群效应：临时顺序节点 + 只监听前驱，避免所有等待者同时被唤醒。</li>
 *   <li>高并发短临界区更推荐 Redis 锁（性能更好）；ZK 锁胜在强一致与自动释放。</li>
 * </ul>
 */
public class LockDemo {

    /** 在 path 处获取分布式锁，执行 work，最后释放。 */
    public static <T> T withLock(CuratorFramework client, String path, Supplier<T> work) throws Exception {
        InterProcessMutex lock = new InterProcessMutex(client, path);
        if (!lock.acquire(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("获取分布式锁超时：" + path);
        }
        try {
            System.out.printf("[zk-lock] 获得锁 %s，执行临界区%n", path);
            return work.get();
        } finally {
            lock.release();
            System.out.printf("[zk-lock] 释放锁 %s%n", path);
        }
    }

    public static <T> T withLockDefault(CuratorFramework client, Supplier<T> work) throws Exception {
        return withLock(client, ZkConstant.LOCK_PATH, work);
    }
}
