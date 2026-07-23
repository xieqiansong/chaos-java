package lan.chaos.sentinel.common.config;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lan.chaos.sentinel.common.constant.SentinelConstants;
import lan.chaos.sentinel.flow.FlowControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sentinel-Nacos 持久化校验器（仅 Nacos 开启时运行）。
 * <p>
 * 为什么需要：本 demo 没有 HTTP 接口，无法用 curl 直观观察限流效果。
 * 此 Runner 在应用启动完成后，对 {@code flow-qps} 资源连续调用 10 次，打印"通过/限流"计数，
 * 让你在控制台直接看到 Nacos 下发的规则是否真正生效，无需额外操作。
 * <p>
 * 判读：若 Nacos 已下发 count=2 的流控规则，应通过≈2、限流≈8；
 * 若 10 次全部通过，说明 Nacos 未下发该资源规则（或 Nacos 不可达、数据源挂载失败）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sentinel.nacos.enabled", havingValue = "true")
public class NacosRuleVerifyRunner implements CommandLineRunner {

    private final FlowControlService flowControlService;

    public NacosRuleVerifyRunner(FlowControlService flowControlService) {
        this.flowControlService = flowControlService;
    }

    @Override
    public void run(String... args) {
        log.info("========== Sentinel-Nacos 规则校验 ==========");
        log.info("当前流控规则数: {}", FlowRuleManager.getRules().size());
        FlowRuleManager.getRules().forEach(r ->
                log.info("  - 资源={} grade={} count={}", r.getResource(), r.getGrade(), r.getCount()));

        int passed = 0;
        int blocked = 0;
        for (int i = 0; i < 10; i++) {
            if ("passed".equals(flowControlService.qpsLimited())) {
                passed++;
            } else {
                blocked++;
            }
        }
        log.info("[校验] 对资源 {} 连续调用 10 次 -> 通过={}, 限流={}",
                SentinelConstants.FLOW_QPS, passed, blocked);
        log.info("  判读: count=2 规则生效时应 通过≈2/限流≈8；若全部通过说明 Nacos 未下发该规则");
        log.info("=============================================");
    }
}
