package lan.chaos.nacos.consumer;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 消费端配置。
 *
 * <p>{@link LoadBalanced} 让 {@link RestTemplate} 具备"按服务名调用 + 客户端负载均衡"能力：
 * 可以直接写 {@code http://nacos-provider/user/1}，由 Spring Cloud LoadBalancer 从 Nacos
 * 拉取实例列表并选择一个真实地址。</p>
 */
@Configuration
public class ConsumerConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
