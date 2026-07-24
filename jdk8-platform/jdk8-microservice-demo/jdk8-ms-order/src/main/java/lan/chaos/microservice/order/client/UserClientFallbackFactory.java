package lan.chaos.microservice.order.client;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.feign.fallback.AbstractFallbackFactory;
import lan.chaos.microservice.common.feign.fallback.FallbackResults;
import lan.chaos.microservice.order.model.UserDTO;
import org.springframework.stereotype.Component;

/**
 * user 服务不可用时（异常 / 熔断）的降级工厂。
 *
 * <p>实现 {@link AbstractFallbackFactory}：拿到 Throwable 记日志，返回一个“假”的 {@link UserClient}，
 * 其方法统一返回 {@link FallbackResults#degraded()} 的 503 R。这样 order 自身不会因 user 挂掉而 500。</p>
 */
@Component
public class UserClientFallbackFactory extends AbstractFallbackFactory<UserClient> {

    @Override
    protected String targetService() {
        return "ms-user";
    }

    @Override
    protected UserClient createFallback(Throwable cause) {
        return new UserClient() {
            @Override
            public R<UserDTO> getUser(Long id) {
                return FallbackResults.degraded("ms-user#getUser(" + id + ")");
            }
        };
    }
}
