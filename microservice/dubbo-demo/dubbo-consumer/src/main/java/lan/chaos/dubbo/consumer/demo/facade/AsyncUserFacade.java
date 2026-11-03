package lan.chaos.dubbo.consumer.demo.facade;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 异步调用 facade（点对点直连 20881）。
 *
 * <p>WHY（异步调用）：consumer 端拿到的返回是 {@link CompletableFuture}，调用线程不必阻塞等网络，
 * 可先做别的事，再 {@code .get()} 或 {@code .thenAccept(...)} 取结果。高并发下能显著减少线程等待，
 * 提升吞吐。Dubbo 以「方法返回 Future」来识别异步，无需额外配置。</p>
 */
@Component
public class AsyncUserFacade {

    @DubboReference(url = "dubbo://127.0.0.1:20880", group = "async")
    private UserService userService;

    public CompletableFuture<User> getByIdAsync(Long id) {
        return userService.getByIdAsync(id);
    }

    /** 可观察输出：等待异步结果后打印。 */
    public String describe(Long id) throws Exception {
        User u = getByIdAsync(id).get();
        return "getByIdAsync(" + id + ") -> " + u + "  (由实例 " + u.getInstanceId() + " 异步处理)";
    }
}
