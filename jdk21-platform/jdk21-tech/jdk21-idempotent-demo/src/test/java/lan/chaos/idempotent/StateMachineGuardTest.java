package lan.chaos.idempotent;

import lan.chaos.idempotent.common.model.BizOrder;
import lan.chaos.idempotent.core.H2IdempotencyStore;
import lan.chaos.idempotent.core.IdempotencyStore;
import lan.chaos.idempotent.core.StateMachineGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 状态机级幂等断言：已终态的单号，重复回调被忽略，不脏写、不重复迁移。
 */
class StateMachineGuardTest {

    private StateMachineGuard guard;

    @BeforeEach
    void setUp() {
        DataSource ds = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2).build();
        IdempotencyStore store = new H2IdempotencyStore(new JdbcTemplate(ds));
        guard = new StateMachineGuard(store);
    }

    @Test
    void repeatedCallback_onTerminalState_isIgnored() {
        BizOrder order = BizOrder.builder().bizNo("BIZ-X").action("CONFIRM").state("CONFIRMED").build();
        BizOrder first = guard.apply(order, o -> o);
        BizOrder second = guard.apply(order, o -> o); // 重复回调

        assertEquals("CONFIRMED", first.getState());
        assertEquals("IGNORED", second.getAction());
        assertEquals(1, guard.appliedCount(), "重复回调不应再次应用状态迁移");
    }
}
