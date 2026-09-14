package lan.chaos.sentinel.common.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import lan.chaos.sentinel.common.constant.SentinelConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sentinel 规则初始化 — 所有规则以编程方式加载，无需 Dashboard。
 * <p>Dashboard 是可选的监控可视化工具（见 docker-compose.yml）。</p>
 *
 * <p><b>与 Nacos 的关系（关键）</b>：本类与 {@link NacosRuleDataSourceConfig} 不能重复灌规则。
 * 两者都用 {@code @PostConstruct}，执行顺序不确定，会互相 {@code loadRules / register2Property} 覆盖，
 * 导致"到底哪份规则生效"无法验证。因此这里规定：<b>仅当 Nacos 持久化关闭时，才用代码初始化规则</b>；
 * Nacos 开启后，规则唯一来源就是 Nacos，本类自动让位，避免冲突。</p>
 *
 * <p>生产环境应通过 {@code ReadableDataSource} 对接 Nacos/Apollo 等配置中心实现规则持久化，
 * 否则应用重启后规则全部丢失。</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "sentinel.nacos.enabled", havingValue = "false", matchIfMissing = true)
public class SentinelRuleConfig {

    @PostConstruct
    public void initRules() {
        loadFlowRules();
        loadDegradeRules();
        loadHotspotRules();
        log.info("Sentinel 规则初始化完成: 流控={} 熔断={} 热点={}",
                FlowRuleManager.getRules().size(),
                DegradeRuleManager.getRules().size(),
                ParamFlowRuleManager.getRules().size());
    }

    /**
     * 流控规则：
     * 1. QPS 直接限流 — count=2，超限快速失败
     * 2. WarmUp 预热 — count=100，前 10s 从 count/3 缓慢爬升至阈值
     * 3. 关联限流 — trigger QPS>2 时连带限制 ref
     */
    private void loadFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule qpsRule = new FlowRule(SentinelConstants.FLOW_QPS);
        qpsRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        qpsRule.setCount(2);
        rules.add(qpsRule);

        FlowRule warmUpRule = new FlowRule(SentinelConstants.FLOW_WARMUP);
        warmUpRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        warmUpRule.setCount(100);
        warmUpRule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP);
        warmUpRule.setWarmUpPeriodSec(10);
        rules.add(warmUpRule);

        FlowRule refRule = new FlowRule(SentinelConstants.FLOW_REF);
        refRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        refRule.setCount(5);
        refRule.setStrategy(RuleConstant.STRATEGY_RELATE);
        refRule.setRefResource(SentinelConstants.FLOW_REF_TRIGGER);
        rules.add(refRule);

        FlowRuleManager.loadRules(rules);
    }

    /**
     * 熔断规则：覆盖异常比例 / 异常数 / 慢调用比例三种策略。
     * <p>熔断打开后 5s 进入半开状态，放行一个请求探测；成功则关闭，失败则继续熔断。</p>
     */
    private void loadDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 异常比例：窗口内异常比例 > 0.5 且请求数 ≥ 3 → 熔断
        DegradeRule erRule = new DegradeRule(SentinelConstants.DEGRADE_EXCEPTION_RATIO);
        erRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        erRule.setCount(0.5);
        erRule.setMinRequestAmount(3);
        erRule.setStatIntervalMs(1000);
        erRule.setTimeWindow(5);
        rules.add(erRule);

        // 异常数：窗口内异常数 ≥ 3 → 熔断（适用于异常率天然低的接口）
        DegradeRule ecRule = new DegradeRule(SentinelConstants.DEGRADE_EXCEPTION_COUNT);
        ecRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);
        ecRule.setCount(3);
        ecRule.setMinRequestAmount(3);
        ecRule.setStatIntervalMs(1000);
        ecRule.setTimeWindow(5);
        rules.add(ecRule);

        // 慢调用比例：窗口内 RT > 200ms 的比例 > 0.5 且请求数 ≥ 3 → 熔断
        DegradeRule srRule = new DegradeRule(SentinelConstants.DEGRADE_SLOW_RATIO);
        srRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        srRule.setCount(200); // RT 阈值 200ms
        srRule.setSlowRatioThreshold(0.5);
        srRule.setMinRequestAmount(3);
        srRule.setStatIntervalMs(1000);
        srRule.setTimeWindow(5);
        rules.add(srRule);

        DegradeRuleManager.loadRules(rules);
    }

    /**
     * 热点参数规则：对不同参数值设置不同的 QPS 阈值。
     * <p>默认 QPS=2，特定参数值 ("vip") 放宽至 QPS=100。</p>
     */
    private void loadHotspotRules() {
        ParamFlowRule rule = new ParamFlowRule(SentinelConstants.HOTSPOT_PARAM);
        rule.setParamIdx(0); // 对第 0 个参数限流
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(2);
        // 特例：参数值 "vip" 不限流
        ParamFlowItem vipItem = new ParamFlowItem();
        vipItem.setObject("vip");
        vipItem.setClassType(String.class.getName());
        vipItem.setCount(100);
        rule.setParamFlowItemList(Collections.singletonList(vipItem));

        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
    }
}
