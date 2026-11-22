package lan.chaos.websockettunnel.common.config;

import lan.chaos.websockettunnel.common.constant.TunnelConstants;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 隧道的可配置项。原项目把端口写死在命令行、帧大小散落在 handler 里，
 * 这里统一收敛成 @ConfigurationProperties，便于部署时覆盖且与网关配置同源。
 */
@Data
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = TunnelConstants.PROPERTY_PREFIX)
public class TunnelProperties {

    /** 是否在同 JVM 内启动隧道服务；关掉后网关的 ws 路由将不可用。 */
    private boolean enabled = true;

    /** 只绑回环地址，避免 7002 直接暴露到外网。 */
    private String host = "127.0.0.1";

    /** 内嵌 WS 服务端口；0 表示随机端口（测试用）。 */
    private int port = 7002;

    /** WS 帧与 HTTP 聚合上限，8MB；高并发场景应结合内存限流调小。 */
    private int maxFrameSize = 8 * 1024 * 1024;

    /** 连接目标 TCP 的超时。 */
    private int connectTimeoutMs = 3000;

    /** 网关握手鉴权用的 token；留空表示关闭鉴权（仅限本地演示）。 */
    private String accessToken = "demo-token";

    /** 目标白名单，默认只放行本机 MySQL/Redis 示例端口。 */
    private List<String> allowedTargets = new ArrayList<>();
}
