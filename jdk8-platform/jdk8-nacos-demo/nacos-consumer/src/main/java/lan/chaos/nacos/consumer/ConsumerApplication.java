package lan.chaos.nacos.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 服务消费者启动类。
 *
 * <p>{@link EnableFeignClients} 开启 Feign 扫描；由于 {@code UserClient} 在 nacos-common 包下，
 * 需显式指定 {@code basePackages} 让 Feign 能扫描到跨模块的客户端接口。</p>
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "lan.chaos.nacos.common")
@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
