package lan.chaos.websockettunnel.common.constant;

/**
 * 隧道相关的常量集中在此，杜绝魔法值散落在 handler / filter 里。
 */
public final class TunnelConstants {

    /** WS 路径前缀，格式 /forward/{host}/{port}。 */
    public static final String FORWARD_PREFIX = "/forward/";

    /** 配置属性前缀。 */
    public static final String PROPERTY_PREFIX = "tunnel.embedded";

    /** 网关鉴权读取的 query 参数名（浏览器 WebSocket 不能自定义 Header）。 */
    public static final String TOKEN_PARAM = "token";

    private TunnelConstants() {
    }
}
