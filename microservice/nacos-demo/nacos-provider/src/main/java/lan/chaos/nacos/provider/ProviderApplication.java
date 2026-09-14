package lan.chaos.nacos.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 服务提供者启动类。
 *
 * <p>{@link EnableDiscoveryClient} 在 Spring Cloud 2020+ 后其实可省略（starter 会自动装配），
 * 这里显式标注是为了让"注册到 Nacos"这一意图更直观。启动后可在 Nacos 控制台
 * 「服务管理 - 服务列表」看到名为 {@code nacos-provider} 的实例。</p>
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
