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
 * 基础 provider（点对点演示用）：导出在 20880，供「基础同步调用」场景直连。
 */
@DubboService(group = "basic")
@Component
public class BasicProvider implements UserService {

    @Override
    public User getById(Long id) {
        return User.sample(id, "direct-basic");
    }

    @Override
    public List<User> list() {
        return LongStream.rangeClosed(1, 3)
                .mapToObj(id -> User.sample(id, "direct-basic"))
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<User> getByIdAsync(Long id) {
        return CompletableFuture.completedFuture(User.sample(id, "direct-basic"));
    }
}
