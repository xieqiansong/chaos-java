package lan.chaos.microservice.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动类。
 * WebFlux 体系，不能与 spring-boot-starter-web 共存；本模块仅依赖 common-core / common-log（无 Servlet）。
 * 路由、鉴权、限流在后续阶段（P1/P2/P4）补充。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
