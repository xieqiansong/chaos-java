package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.common.context.DynamicTableContext;
import lan.chaos.mybatisplus.common.context.TenantContext;
import lan.chaos.mybatisplus.dynamictable.DynamicTableScenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DynamicTableScenarioTest {

    @Autowired
    private DynamicTableScenario dynamicTableScenario;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        DynamicTableContext.clear();
    }

    @Test
    void routeByYear() {
        Map<String, Object> r = dynamicTableScenario.routeByYear();
        assertThat((Boolean) r.get("has2024new")).isTrue();
        assertThat((Boolean) r.get("has2025new")).isTrue();
        assertThat((Boolean) r.get("noCrossYear")).isTrue(); // 路由正确，不串表
    }
}
