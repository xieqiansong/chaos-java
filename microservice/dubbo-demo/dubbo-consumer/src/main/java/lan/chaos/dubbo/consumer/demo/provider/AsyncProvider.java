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
 * 异步 provider（点对点演示用）：导出在 20881。
 *
 * <p>{@link #getByIdAsync} 用 {@code CompletableFuture.supplyAsync} 把构造结果的逻辑异步化，
 * Dubbo 据此识别为异步调用，consumer 拿到 Future 后可非阻塞等待。</p>
 */
@DubboService(group = "async")
@Component
public class AsyncProvider implements UserService {

    @Override
    public User getById(Long id) {
        return User.sample(id, "direct-async");
    }

    @Override
    public List<User> list() {
        return LongStream.rangeClosed(1, 3)
                .mapToObj(id -> User.sample(id, "direct-async"))
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<User> getByIdAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> User.sample(id, "direct-async"));
    }
}
