package lan.chaos.zookeeper.config;

import lan.chaos.zookeeper.common.constant.ZkConstant;
import lan.chaos.zookeeper.common.util.ZkClientUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置中心 + Watcher 场景测试：依赖真实 ZooKeeper。
 * 无 ZK 时用 JUnit {@link Assumptions} 优雅跳过。
 */
public class ConfigDemoTest {

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
    void readConfig_roundTrip() throws Exception {
        String path = "/chaos/config/test-flag";
        client.create().creatingParentsIfNeeded().forPath(path, "on".getBytes(StandardCharsets.UTF_8));
        String v = ConfigDemo.readConfig(client, path, "off");
        assertEquals("on", v);
    }

    @Test
    void watchConfig_receivesUpdatePush() throws Exception {
        String path = "/chaos/config/test-watch";
        client.create().creatingParentsIfNeeded().forPath(path, "v1".getBytes(StandardCharsets.UTF_8));

        List<String> received = new ArrayList<>();
        NodeCache cache = ConfigDemo.watchConfig(client, path, "v1", received::add);

        // 外部把配置改成 v2，Watcher（NodeCache）应把新值推下来
        client.setData().forPath(path, "v2".getBytes(StandardCharsets.UTF_8));
        Thread.sleep(500);
        cache.close();

        assertTrue(received.contains("v2"), "配置变更应被 Watcher 推送到监听方，实际收到=" + received);
    }
}
