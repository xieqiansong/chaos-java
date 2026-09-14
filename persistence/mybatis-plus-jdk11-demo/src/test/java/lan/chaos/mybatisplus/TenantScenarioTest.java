package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.tenant.TenantScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多租户隔离：不同租户视角只可见各自数据。
 */
@SpringBootTest
@Transactional
class TenantScenarioTest {

    @Autowired
    private TenantScenario tenantScenario;

    @Test
    void tenantIsolation() {
        Map<String, Object> r = tenantScenario.tenantIsolation();

        assertTrue((Boolean) r.get("tenant1AllSameTenant"));
        assertTrue((Boolean) r.get("tenant2AllSameTenant"));
    }
}
