package lan.chaos.dubbo.provider;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.constant.DubboConstants;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * 能力一：服务暴露（@DubboService）。
 *
 * <p>WHY：{@link DubboService} 把一个普通 Spring Bean 暴露成「远程服务」。
 * Dubbo 在启动时为它生成 exporter，监听 {@code dubbo://:20880}，并把服务元数据
 * （接口名、方法、协议、地址）注册到 Nacos。consumer 端拿到的是一个「动态代理」，
 * 调用方法实际走网络——但代码写起来和调本地方法一模一样。</p>
 *
 * <p>关键 API：{@code @DubboService} 的 {@code version} / {@code group} / {@code timeout}
 * / {@code retries} 等属性，分别对应灰度、隔离、超时、重试等生产配置。</p>
 *
 * <p>生产坑：
 * <ul>
 *   <li>被传输的 {@link User} 必须实现 {@code Serializable}（已满足），否则序列化即报错；</li>
 *   <li>接口要「稳定」，字段增删会破坏两端兼容，建议用专门的 RPC DTO，而非业务实体；</li>
 *   <li>方法参数/返回值尽量避免超大对象，Hessian2 序列化 + 网络往返成本随体积线性上升。</li>
 * </ul>
 */
@DubboService
@Component
public class UserServiceImpl implements UserService {

    /** 实例标识：默认 {@code provider-20880}；真实多实例负载均衡演示时可用环境变量区分。 */
    @Value("${dubbo.instance-id:provider-20880}")
    private String instanceId;

    @Override
    public User getById(Long id) {
        // 同步方法：直接返回样例用户，并打上「本实例标识」，便于 consumer 观察请求落在哪个实例
        return User.sample(id, instanceId);
    }

    @Override
    public List<User> list() {
        return LongStream.rangeClosed(1, 3)
                .mapToObj(id -> User.sample(id, instanceId))
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<User> getByIdAsync(Long id) {
        // 异步方法：用 supplyAsync 把耗时逻辑丢到 ForkJoinPool，立即返回 Future。
        // Dubbo 识别到返回值是 CompletableFuture，自动按「异步调用」处理，
        // consumer 端拿到的也是 Future，可 .get() 阻塞或 .thenAccept() 回调。
        return CompletableFuture.supplyAsync(() -> User.sample(id, instanceId));
    }
}
