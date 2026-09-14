package lan.chaos.zookeeper.common.constant;

/**
 * ZooKeeper 演示常量：默认连接串、各能力使用的 znode 路径。杜绝魔法值。
 */
public final class ZkConstant {

    private ZkConstant() {}

    /** 默认连接串（docker-compose 起的单机 ZK）。 */
    public static final String CONNECT_STRING = "REDACTED:2181;REDACTED:2182;REDACTED:2183";

    /** 分布式锁根路径。 */
    public static final String LOCK_PATH = "/chaos/locks/order";
    /** Leader 选举根路径。 */
    public static final String LEADER_PATH = "/chaos/leader";
    /** 配置中心节点路径。 */
    public static final String CONFIG_PATH = "/chaos/config/feature-flag";
}
