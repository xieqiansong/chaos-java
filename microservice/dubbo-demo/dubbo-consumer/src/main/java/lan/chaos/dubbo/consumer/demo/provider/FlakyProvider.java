package lan.chaos.dubbo.consumer.demo.provider;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.constant.DubboConstants;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * 故障 provider（点对点演示用）：group=fault，导出在 20880。
 *
 * <p>WHY（集群容错场景）：前 2 次调用故意「阻塞超过 consumer 超时」模拟 provider 响应超时，
 * consumer 端配 {@code cluster=failover, retries=2, timeout=500ms} 时会自动重试，第 3 次成功。
 * 用 {@link AtomicInteger} 记录实际被调用的次数，成功时把次数写进 {@code age} 字段，
 * 便于 consumer 观察「确实发生了重试并恢复」。</p>
 *
 * <p>注：cluster=failover 默认重试非业务异常（如超时 / 网络故障）；本 demo 用真实超时触发，
 * 最贴近生产「provider 抖动 → 自动重试恢复」的真实链路。</p>
 */
@DubboService(group = "fault")
@Component
public class FlakyProvider implements UserService {

    private final AtomicInteger attempts = new AtomicInteger(0);

    @Override
    public User getById(Long id) {
        int n = attempts.incrementAndGet();
        if (n < 3) {
            // 前 2 次阻塞 2s，超过 consumer 的 500ms 超时，触发 failover 重试
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // 第 3 次成功：把重试次数（3）放进 age，便于观察
        User u = User.sample(id, "direct-fault");
        u.setAge(n);
        return u;
    }

    @Override
    public List<User> list() {
        return LongStream.rangeClosed(1, 3)
                .mapToObj(id -> User.sample(id, "direct-fault"))
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<User> getByIdAsync(Long id) {
        return CompletableFuture.completedFuture(User.sample(id, "direct-fault"));
    }
}
