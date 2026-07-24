package lan.chaos.microservice.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动类。创建 / 查询订单，Feign 编排调 user（P1 落地），跨服务事务接 Seata（P3）。
 * basePackages 覆盖 common-feign 中的 Feign 客户端与配置。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "lan.chaos.microservice.common.feign")
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
