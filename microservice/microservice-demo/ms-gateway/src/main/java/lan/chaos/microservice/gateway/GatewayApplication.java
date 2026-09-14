package lan.chaos.microservice.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 网关入口（WebFlux，不依赖 spring-boot-starter-web）。
 *
 * <p>{@code @ComponentScan("lan.chaos.microservice")} 纳管 common 包配置（common-log 的日志配置等）。
 * 网关本身不写业务 Controller，只做路由转发 + Sentinel 流控（见 {@code config/SentinelGatewayConfig}）。</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("lan.chaos.microservice")
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
