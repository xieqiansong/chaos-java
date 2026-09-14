package lan.chaos.zookeeper;

import lan.chaos.zookeeper.common.constant.ZkConstant;
import lan.chaos.zookeeper.common.util.ZkClientUtil;
import lan.chaos.zookeeper.config.ConfigDemo;
import lan.chaos.zookeeper.leader.LeaderDemo;
import lan.chaos.zookeeper.lock.LockDemo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;

/**
 * ZooKeeper Demo 启动类。需先起 ZK（见 docker-compose.yml）：
 *
 * <pre>
 *   docker compose up -d
 *   mvn -pl jdk8-zookeeper-demo spring-boot:run
 * </pre>
 *
 * 演示：分布式锁、Leader 选举、配置中心 + Watcher（控制台观察「输入→输出」）。
 */
public class ZookeeperDemoApplication {
    public static void main(String[] args) throws Exception {
        CuratorFramework client = ZkClientUtil.newClient(ZkConstant.CONNECT_STRING);
        client.blockUntilConnected(5, java.util.concurrent.TimeUnit.SECONDS);

        // 1) 分布式锁
        String r = LockDemo.withLockDefault(client, () -> "critical-work-done");
        System.out.println("[main] 锁内结果=" + r);

        // 2) Leader 选举
        boolean leader = LeaderDemo.electAndHoldDefault(client);
        System.out.println("[main] 是否曾当选 Leader=" + leader);

        // 3) 配置中心 + Watcher
        String cfg = ConfigDemo.readConfigDefault(client);
        System.out.println("[main] 当前配置=" + cfg);
        NodeCache cache = ConfigDemo.watchConfigDefault(client, v -> {});
        // 模拟外部把配置改成 v2，Watcher 会推送
        client.setData().forPath(ZkConstant.CONFIG_PATH, "v2".getBytes());
        Thread.sleep(500);
        cache.close();
        client.close();
    }
}
