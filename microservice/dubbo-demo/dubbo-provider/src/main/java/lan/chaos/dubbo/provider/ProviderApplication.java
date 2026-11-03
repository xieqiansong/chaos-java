package lan.chaos.dubbo.provider;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务提供者启动类（角色单元，形态二）。
 *
 * <p>{@link EnableDubbo} 开启 Dubbo 注解扫描：把本模块里标注了 {@code @DubboService} 的 Bean
 * 导出为远程服务（监听 {@code dubbo://:20880}），并连上 Nacos 注册中心登记自身实例。
 * consumer 启动后即可从 Nacos 发现本实例并发起跨进程调用。</p>
 *
 * <p>运行前请先 {@code docker-compose up -d} 起 Nacos（见 dubbo-demo/docker-compose.yml）。</p>
 */
@EnableDubbo(scanBasePackages = "lan.chaos.dubbo")
@SpringBootApplication
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
