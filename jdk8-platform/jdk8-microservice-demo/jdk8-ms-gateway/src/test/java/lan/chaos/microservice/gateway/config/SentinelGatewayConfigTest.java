package lan.chaos.microservice.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SentinelGatewayConfigTest {

    @Test
    void buildGatewayRules_hasUserAndOrderRouteWithQpsLimit() {
        Set<GatewayFlowRule> rules = SentinelGatewayConfig.buildGatewayRules();
        assertEquals(2, rules.size());

        long userRoute = rules.stream()
                .filter(r -> "ms-user-route".equals(r.getResource()) && r.getCount() == 5)
                .count();
        assertEquals(1, userRoute);

        long orderRoute = rules.stream()
                .filter(r -> "ms-order-route".equals(r.getResource()) && r.getCount() == 5)
                .count();
        assertEquals(1, orderRoute);
    }
}
