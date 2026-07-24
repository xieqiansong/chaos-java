package lan.chaos.microservice.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.core.result.ResultCode;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * 网关 Sentinel 流控配置。
 *
 * <p>WHY：网关是流量的“总闸”，必须在入口做限流，避免突发流量打垮后端。
 * 这里用「路由维度」的 QPS 流控规则：对 {@code ms-user-route} / {@code ms-order-route} 限流，
 * 超限直接返回 429 + 约定 R，而不是把压力转嫁给后端服务。</p>
 *
 * <p>关键 API：
 * <ul>
 *   <li>{@link GatewayRuleManager#loadRules(Set)} 加载路由维度流控规则；
 *       流控过滤器由 spring-cloud-alibaba 的 Sentinel 网关自动配置提供（{@code SentinelGatewayFilter}）。</li>
 *   <li>自定义限流响应走 {@link GatewayCallbackManager#setBlockHandler(BlockRequestHandler)} 注册，
 *       把默认的纯文本改成统一的 429 + 约定 {@link R}（处理器本身由 SCA 自动配置提供，无需重复定义）。</li>
 * </ul>
 */
@Configuration
public class SentinelGatewayConfig {

    /** 路由 ID，必须与 application.yml 中 gateway.routes[].id 对应 */
    private static final String USER_ROUTE = "ms-user-route";
    private static final String ORDER_ROUTE = "ms-order-route";

    /** 1 秒内允许的最大请求数（演示用，生产按压测调） */
    private static final int QPS_LIMIT = 5;

    /**
     * 构建网关流控规则（抽成静态方法便于单测断言，不依赖 Spring 上下文）。
     */
    public static Set<GatewayFlowRule> buildGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        // 路由维度 QPS 限流：resource 即路由 id，resourceMode=ROUTE_ID
        rules.add(new GatewayFlowRule(USER_ROUTE)
                .setCount(QPS_LIMIT)
                .setIntervalSec(1)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID));
        rules.add(new GatewayFlowRule(ORDER_ROUTE)
                .setCount(QPS_LIMIT)
                .setIntervalSec(1)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_ROUTE_ID));
        return rules;
    }

    @PostConstruct
    public void initGatewayRules() {
        GatewayRuleManager.loadRules(buildGatewayRules());
    }

    @PostConstruct
    public void initBlockHandler() {
        // 超限响应：429 + 约定 R（否则默认是纯文本 "Blocked by Sentinel: FlowException"）
        BlockRequestHandler handler = (exchange, throwable) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(R.fail(ResultCode.GATEWAY_BLOCKED));
        GatewayCallbackManager.setBlockHandler(handler);
    }
}
