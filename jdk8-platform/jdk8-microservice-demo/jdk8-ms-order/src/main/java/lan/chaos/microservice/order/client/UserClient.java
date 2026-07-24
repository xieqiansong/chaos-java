package lan.chaos.microservice.order.client;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.order.model.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 调用 user 服务的 Feign 客户端。
 *
 * <p>{@code name="ms-user"} 走 Nacos 服务发现 + Sentinel 熔断；{@code fallbackFactory} 指定降级工厂，
 * 服务不可用时返回 {@link R} 兜底而非抛异常。{@code path="/users"} 与服务端 {@code UserController} 前缀一致。</p>
 */
@FeignClient(name = "ms-user", path = "/users", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/{id}")
    R<UserDTO> getUser(@PathVariable("id") Long id);
}
