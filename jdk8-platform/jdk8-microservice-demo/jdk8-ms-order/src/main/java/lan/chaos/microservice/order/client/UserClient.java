package lan.chaos.microservice.order.client;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.order.model.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * 调用 user 服务的 Feign 客户端。
 *
 * <p>{@code name="ms-user"} 走 Nacos 服务发现 + Sentinel 熔断；{@code fallbackFactory} 指定降级工厂，
 * 服务不可用时返回 {@link R} 兜底而非抛异常。{@code path="/users"} 与服务端 {@code UserController} 前缀一致。</p>
 *
 * <p>分布式事务相关：{@link #deductAccount} 在 {@code @GlobalTransactional} 内被调用时，Seata 的 Feign 拦截器
 * 会自动把 xid 放进请求头透传到 user，使对端扣减成为同一全局事务的分支。</p>
 */
@FeignClient(name = "ms-user", path = "/users", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {

    @GetMapping("/{id}")
    R<UserDTO> getUser(@PathVariable("id") Long id);

    /** 扣减账户余额（Seata 全局事务的用户侧分支）。余额不足时 user 返回 409 R。 */
    @PostMapping("/{id}/account/deduct")
    R<Void> deductAccount(@PathVariable("id") Long id, @RequestParam("amount") BigDecimal amount);
}
