package lan.chaos.mybatisplus;

import lan.chaos.mybatisplus.audit.AuditScenario;
import lan.chaos.mybatisplus.common.context.DynamicTableContext;
import lan.chaos.mybatisplus.common.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuditScenarioTest {

    @Autowired
    private AuditScenario auditScenario;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        DynamicTableContext.clear();
    }

    @Test
    void logicDeleteAndVersion() {
        Map<String, Object> r = auditScenario.logicDeleteAndVersion();
        assertThat(r.get("updatedRows")).isEqualTo(1);
        assertThat(r.get("versionAfterUpdate")).isEqualTo(2);   // 乐观锁自增
        assertThat(r.get("deletedRows")).isEqualTo(1);
        assertThat((Boolean) r.get("afterDeleteVisible")).isFalse(); // 逻辑删除后不可见
        assertThat(r.get("operatorFilled")).isNotNull();        // 自动填充生效
    }
}
