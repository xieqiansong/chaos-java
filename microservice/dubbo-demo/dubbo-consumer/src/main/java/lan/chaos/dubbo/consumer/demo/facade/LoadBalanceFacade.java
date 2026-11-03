package lan.chaos.dubbo.consumer.demo.facade;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 负载均衡 facade（点对点演示两个实例 lbA / lbB）。
 *
 * <p>WHY（负载均衡）：当同一服务有多个 provider 实例时，consumer 端决定「这次请求发给谁」，
 * 由 {@code loadbalance} 指定策略：{@code random}（默认，按权重）、{@code roundrobin}（轮询）、
 * {@code leastactive}（最活跃优先）、{@code consistenthash}（一致性哈希）。</p>
 *
 * <p>真实形态下实例列表来自注册中心（Nacos），Dubbo 内置的 {@code loadbalance} 直接在「实例列表」
 * 上做选择——本 demo 的 {@code application-nacos.yml} + 多 provider 进程即该形态。
 * 单 JVM 点对点演示无法在同一接口上挂两个注册实例，故这里用两个独立 group 实例
 * （lbA=20883 / lbB=20884）等价模拟「两个实例」，并在 consumer 侧交替调用，
 * 观察请求确实被分摊到 A / B（生产里这一步由 Dubbo 的 {@code loadbalance} 自动完成）。</p>
 */
@Component
public class LoadBalanceFacade {

    @DubboReference(url = "dubbo://127.0.0.1:20880", group = "lbA", loadbalance = "roundrobin")
    private UserService userServiceA;

    @DubboReference(url = "dubbo://127.0.0.1:20880", group = "lbB", loadbalance = "roundrobin")
    private UserService userServiceB;

    /** 交替调用两个实例 n 次，收集每次实际落到的实例标识。 */
    public List<String> whichInstances(int n) {
        List<String> hits = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            UserService target = (i % 2 == 0) ? userServiceA : userServiceB;
            hits.add(target.getById(1L).getInstanceId());
        }
        return hits;
    }

    /** 可观察输出：打印各实例命中次数分布。 */
    public String describe(int n) {
        List<String> hits = whichInstances(n);
        long a = hits.stream().filter("A"::equals).count();
        long b = hits.stream().filter("B"::equals).count();
        return "连续调用 " + n + " 次 -> A 命中 " + a + " 次, B 命中 " + b + " 次 (roundrobin 均衡)";
    }
}
