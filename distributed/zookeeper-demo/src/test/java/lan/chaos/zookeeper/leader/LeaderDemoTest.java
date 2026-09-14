package lan.chaos.zookeeper.leader;

import lan.chaos.zookeeper.common.constant.ZkConstant;
import lan.chaos.zookeeper.common.util.ZkClientUtil;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leader 选举场景测试：依赖真实 ZooKeeper。
 * 无 ZK 时用 JUnit {@link Assumptions} 优雅跳过。
 */
public class LeaderDemoTest {

    private CuratorFramework client;

    @BeforeAll
    static void needZk() {
        Assumptions.assumeTrue(ZkClientUtil.isAvailable(ZkConstant.CONNECT_STRING),
                "跳过：本机无 ZooKeeper（先 `docker compose up -d` 再跑测试才会执行）");
    }

    @BeforeEach
    void setUp() {
        client = ZkClientUtil.newClient(ZkConstant.CONNECT_STRING);
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void electAndHold_singleInstance_becomesLeader() throws Exception {
        assertTrue(LeaderDemo.electAndHoldDefault(client), "单机环境下本实例应当选 Leader");
    }
}
