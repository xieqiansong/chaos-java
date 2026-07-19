package lan.chaos.nacos.common;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 声明式 HTTP 客户端：消费方通过它调用 nacos-provider，无需手写 URL 与负载均衡逻辑。
 *
 * <p>{@code name} 为被调服务在 Nacos 中注册的服务名（spring.application.name）。
 * Feign 会自动结合 Spring Cloud LoadBalancer 从 Nacos 实例列表中做负载均衡。</p>
 */
@FeignClient(name = "nacos-provider")
public interface UserClient {

    @GetMapping("/user/{id}")
    User getById(@PathVariable("id") Long id);
}
