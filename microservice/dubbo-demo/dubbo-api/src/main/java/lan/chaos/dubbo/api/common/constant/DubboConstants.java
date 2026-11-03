package lan.chaos.dubbo.api.common.constant;

/**
 * Dubbo 公共常量（杜绝魔法值）。
 *
 * <p>集中管理协议名、默认端口、注册中心地址与服务名，provider / consumer / 测试统一引用，
 * 改一处即可全局生效。</p>
 */
public final class DubboConstants {

    private DubboConstants() {
    }

    /** Dubbo 协议名（默认高性能二进制协议，基于 Netty）。 */
    public static final String PROTOCOL_DUBBO = "dubbo";

    /** provider 默认导出端口（真实多进程形态）。 */
    public static final int PROVIDER_PORT = 20880;

    /** 单进程点对点演示用的多端口区间（基础/异步/容错/负载均衡 A/B）。 */
    public static final int PORT_BASIC = 20880;
    public static final int PORT_ASYNC = 20881;
    public static final int PORT_FAULT = 20882;
    public static final int PORT_LB_A = 20883;
    public static final int PORT_LB_B = 20884;

    /** Nacos 注册中心地址（与 nacos-demo 同栈，本地 standalone 默认 8848）。 */
    public static final String NACOS_ADDRESS = "nacos://127.0.0.1:8848";

    /** 应用 / 服务名。 */
    public static final String APPLICATION_NAME = "dubbo-provider";
    public static final String CONSUMER_NAME = "dubbo-consumer";
}
