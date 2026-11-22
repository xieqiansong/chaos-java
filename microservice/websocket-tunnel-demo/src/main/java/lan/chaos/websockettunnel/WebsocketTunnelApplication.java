package lan.chaos.websockettunnel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关 + 内嵌 TCP over WebSocket 隧道的演示入口。
 *
 * <p>关键点：本应用本身就是 Spring Cloud Gateway（8080），同时在同一 JVM 内再起一个
 * Netty WS 服务（默认只绑 127.0.0.1:7002）。两者各用各的 EventLoopGroup、各占各的端口，
 * 网关通过 {@code ws://} 路由把握手流量代理到本地 7002——这是「代码引入 + 独立端口」的组合形态，
 * 既避免把自定义 handler 塞进 Reactor Netty 的 pipeline，又保持单进程部署。</p>
 */
@SpringBootApplication
public class WebsocketTunnelApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsocketTunnelApplication.class, args);
    }
}
