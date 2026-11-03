package lan.chaos.dubbo.consumer.demo.facade;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 集群容错 facade（点对点直连 20880）。
 *
 * <p>WHY（集群容错）：分布式调用必然遇到 provider 偶发失败 / 超时。Dubbo 在 consumer 端内置多种
 * 集群策略，由 {@code cluster} 指定：
 * <ul>
 *   <li>{@code failover}（默认）：失败（如超时）重试其他实例，{@code retries} 控制重试次数；</li>
 *   <li>{@code failfast}：快速失败，不重试（写操作常用）；</li>
 *   <li>{@code failsafe}：失败忽略（日志/统计等不影响主流程的场景）；</li>
 *   <li>{@code forking}：并行发多个，任一成功即返回；{@code broadcast}：广播给所有实例。</li>
 * </ul>
 * 这里用 {@code failover,retries=2,timeout=500ms}：provider 前 2 次阻塞超过 500ms 触发超时，
 * failover 自动重试；第 3 次及时返回，整体调用仍成功。
 * 返回的 {@code age} 字段即「实际重试次数（3）」，可观察重试恢复效果。</p>
 */
@Component
public class ClusterFaultTolerantFacade {

    @DubboReference(url = "dubbo://127.0.0.1:20880", group = "fault",
            cluster = "failover", retries = 2, timeout = 500)
    private UserService userService;

    public User getById(Long id) {
        return userService.getById(id);
    }

    /** 可观察输出：返回成功结果 + 重试次数（age 字段）。 */
    public String describe(Long id) {
        User u = getById(id);
        return "getById(" + id + ") -> " + u
                + "  (failover 重试后恢复，累计尝试 " + u.getAge() + " 次)";
    }
}
