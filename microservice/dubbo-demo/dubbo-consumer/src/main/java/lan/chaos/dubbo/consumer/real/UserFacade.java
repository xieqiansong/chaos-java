package lan.chaos.dubbo.consumer.real;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 能力二：服务引用（@DubboReference）。
 *
 * <p>WHY：{@link DubboReference} 在 consumer 端生成一个「指向远程服务的动态代理」。
 * 字段类型写的是本地接口 {@link UserService}，但方法调用会被 Dubbo 拦截、序列化参数、
 * 通过 Netty 发到 provider、再把返回值反序列化回来——整个过程对业务代码透明。</p>
 *
 * <p>关键 API：{@code @DubboReference} 的 {@code check}（启动是否强依赖 provider 在线）、
 * {@code timeout} / {@code retries}（超时与重试）、{@code loadbalance}（负载均衡策略）、
 * {@code cluster}（集群容错策略）等，都是在这里或全局配置的。</p>
 *
 * <p>此处不写 {@code url}，表示「走注册中心发现」；若写成 {@code url="dubbo://host:port"}
 * 则是点对点直连（见 demo 包）。</p>
 */
@Component
public class UserFacade {

    @DubboReference
    private UserService userService;

    public User getById(Long id) {
        return userService.getById(id);
    }

    public List<User> list() {
        return userService.list();
    }

    public CompletableFuture<User> getByIdAsync(Long id) {
        return userService.getByIdAsync(id);
    }

    /** 统一输出「输入→输出」，便于控制台 / 测试观察。 */
    public String describeCall(Long id) {
        User u = getById(id);
        return "getById(" + id + ") -> " + u + "  (由实例 " + u.getInstanceId() + " 处理)";
    }
}
