package lan.chaos.dubbo.consumer.demo.provider;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.constant.DubboConstants;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * 负载均衡 provider A（点对点演示用）：导出在 20883，实例标识 "A"。
 * 与 {@link LoadBalanceProviderB} 共同挂在 {@code UserService} 下，演示 consumer 端负载均衡。
 */
@DubboService(group = "lbA")
@Component
public class LoadBalanceProviderA implements UserService {

    @Override
    public User getById(Long id) {
        return User.sample(id, "A");
    }

    @Override
    public List<User> list() {
        return LongStream.rangeClosed(1, 3)
                .mapToObj(id -> User.sample(id, "A"))
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<User> getByIdAsync(Long id) {
        return CompletableFuture.completedFuture(User.sample(id, "A"));
    }
}
