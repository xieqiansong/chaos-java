package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.dynamictable.DynamicTableScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动态表名分表：验证数据按年份路由到不同物理表，且互不串表。
 */
@SpringBootTest
@Transactional
class DynamicTableScenarioTest {

    @Autowired
    private DynamicTableScenario dynamicTableScenario;

    @Test
    void routeByYear() {
        Map<String, Object> r = dynamicTableScenario.routeByYear();

        assertTrue((Boolean) r.get("has2024new"));
        assertTrue((Boolean) r.get("has2025new"));
        assertTrue((Boolean) r.get("noCrossYear")); // 2024 表不含 2025 数据
    }
}
