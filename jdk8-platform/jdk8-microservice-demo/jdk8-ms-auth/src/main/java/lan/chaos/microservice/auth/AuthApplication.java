package lan.chaos.microservice.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 鉴权服务入口。
 *
 * <p>{@code @ComponentScan("lan.chaos.microservice")} 纳管 common 包配置
 * （common-security 的 {@code JwtAutoConfig} 提供 JwtProvider、common-log 的 TraceIdFilter 等）。</p>
 *
 * <p>{@code @EnableDiscoveryClient} 注册到 Nacos，供网关 {@code lb://ms-auth} 路由。</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("lan.chaos.microservice")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
