package lan.chaos.microservice.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 订单服务入口。
 *
 * <p>{@code @EnableFeignClients} 扫描 {@code lan.chaos.microservice} 下所有 {@code @FeignClient}（如 {@link lan.chaos.microservice.order.client.UserClient}）。
 * {@code @ComponentScan("lan.chaos.microservice")} 同时纳管 common 包里的配置
 * （common-feign 的透传拦截器、common-log 的 TraceIdFilter 等），避免每个服务手动引一遍。</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "lan.chaos.microservice")
@ComponentScan("lan.chaos.microservice")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
