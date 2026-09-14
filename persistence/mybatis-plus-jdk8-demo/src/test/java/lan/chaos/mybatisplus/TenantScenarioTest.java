package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.common.context.DynamicTableContext;
import lan.chaos.mybatisplus.common.context.TenantContext;
import lan.chaos.mybatisplus.tenant.TenantScenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TenantScenarioTest {

    @Autowired
    private TenantScenario tenantScenario;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        DynamicTableContext.clear();
    }

    @Test
    void tenantIsolation() {
        Map<String, Object> r = tenantScenario.tenantIsolation();
        assertThat((Boolean) r.get("tenant1AllSameTenant")).isTrue();
        assertThat((Boolean) r.get("tenant2AllSameTenant")).isTrue();
        assertThat((Integer) r.get("tenant1Count")).isGreaterThan(0);
        assertThat((Integer) r.get("tenant2Count")).isGreaterThan(0);
    }
}
