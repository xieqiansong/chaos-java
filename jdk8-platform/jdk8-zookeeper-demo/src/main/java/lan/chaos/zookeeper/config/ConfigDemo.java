package lan.chaos.zookeeper.config;

import lan.chaos.zookeeper.common.constant.ZkConstant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ★★★ 高频：ZK 配置中心 + Watcher —— 把配置放 ZK 节点，变更时「推」给所有监听方，动态生效。
 *
 * <p>痛点：改配置要重启应用。ZK 的节点 + Watcher 机制让配置集中存储、变更即通知，无需重启。
 * 原生 Watcher 是一次性的（触发后需重新注册），Curator 的 {@link NodeCache} 帮我们持续监听。
 *
 * <p>关键 API：{@code client.setData()/getData()} + {@link NodeCache}（封装一次性 Watcher 的重注册）。
 *
 * <p>生产坑：
 * <ul>
 *   <li>Watcher 一次性：原生每触发一次要重新注册，漏注册就会丢事件——用 NodeCache 省心。</li>
 *   <li>配置要有默认值与本地兜底，ZK 不可用时不能全崩。</li>
 *   <li>大配置别放 ZK 单节点（有 1MB 上限），超大配置用对象存储 + ZK 存指针。</li>
 * </ul>
 */
public class ConfigDemo {

    /** 读取配置节点（不存在则初始化为 defaultValue）。返回当前值。 */
    public static String readConfig(CuratorFramework client, String path, String defaultValue) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            client.create().creatingParentsIfNeeded().forPath(path, defaultValue.getBytes(StandardCharsets.UTF_8));
        }
        return new String(client.getData().forPath(path), StandardCharsets.UTF_8);
    }

    public static String readConfigDefault(CuratorFramework client) throws Exception {
        return readConfig(client, ZkConstant.CONFIG_PATH, "v1");
    }

    /**
     * 监听配置节点：返回 NodeCache（调用方写新值后，onChange 会被触发）。用完请 close。
     */
    public static NodeCache watchConfig(CuratorFramework client, String path, String defaultValue,
                                        Consumer<String> onChange) throws Exception {
        if (client.checkExists().forPath(path) == null) {
            client.create().creatingParentsIfNeeded().forPath(path, defaultValue.getBytes(StandardCharsets.UTF_8));
        }
        NodeCache cache = new NodeCache(client, path);
        List<String> received = new ArrayList<>();
        NodeCacheListener listener = () -> {
            String v = new String(cache.getCurrentData().getData(), StandardCharsets.UTF_8);
            received.add(v);
            onChange.accept(v);
            System.out.printf("[zk-config] 配置变更为 %s（累计收到 %d 次）%n", v, received.size());
        };
        cache.getListenable().addListener(listener);
        cache.start();
        return cache;
    }

    public static NodeCache watchConfigDefault(CuratorFramework client, Consumer<String> onChange) throws Exception {
        return watchConfig(client, ZkConstant.CONFIG_PATH, "v1", onChange);
    }
}
