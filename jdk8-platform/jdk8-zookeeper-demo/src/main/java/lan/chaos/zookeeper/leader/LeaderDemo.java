package lan.chaos.zookeeper.leader;

import lan.chaos.zookeeper.common.constant.ZkConstant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ★★★ 高频：ZK Leader 选举 —— 多实例中选出一个 Leader 承担「只能一个干」的主任务
 * （如全局定时调度、主数据同步、主备切换），其余实例 standby；Leader 宕机导致 session 断开后，
 * ZK 会自动触发重选，保证高可用。
 *
 * <p>痛点：集群里每个实例都想干同一件「只能一个干」的活。谁当 Leader 由 ZK 的顺序临时节点公平裁决，
 * 且失败自动转移，无需人工介入。
 *
 * <p>关键 API：Curator 的 {@link LeaderSelector} + {@link LeaderSelectorListenerAdapter#takeLeadership}；
 * {@code autoRequeue()} 让实例在失去领导权后自动重新排队，等待下次选举。
 *
 * <p>生产坑：
 * <ul>
 *   <li>takeLeadership 不返回就一直持有领导权；释放要靠 selector.close() 中断该线程。</li>
 *   <li>领导权转移有「脑裂窗口」：旧 Leader 还在干活、新 Leader 已上任，业务要幂等或加租约(lease)。</li>
 *   <li>选主只是「选出协调者」，真正的任务分发还要结合业务锁或队列。</li>
 * </ul>
 */
public class LeaderDemo {

    /**
     * 参与一次 Leader 选举：在本进程内选主并短暂持有，返回是否曾当选。
     *
     * @param client 已连接的 Curator 客户端
     * @param path   选主根路径
     * @return 本实例是否曾当选为 Leader
     */
    public static boolean electAndHold(CuratorFramework client, String path) throws Exception {
        AtomicBoolean elected = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        LeaderSelector selector = new LeaderSelector(client, path, new LeaderSelectorListenerAdapter() {
            @Override
            public void takeLeadership(CuratorFramework client) throws Exception {
                elected.set(true);
                System.out.printf("[zk-leader] 本实例当选为 Leader（path=%s），开始执行业务%n", path);
                latch.countDown();
                // 持有领导权直到被 close() 中断（模拟持续干活）
                Thread.sleep(Long.MAX_VALUE);
            }
        });
        // 失去领导权后自动重新排队，等待下次当选
        selector.autoRequeue();
        selector.start();
        try {
            return latch.await(5, TimeUnit.SECONDS) && elected.get();
        } finally {
            selector.close();
        }
    }

    public static boolean electAndHoldDefault(CuratorFramework client) throws Exception {
        return electAndHold(client, ZkConstant.LEADER_PATH);
    }
}
