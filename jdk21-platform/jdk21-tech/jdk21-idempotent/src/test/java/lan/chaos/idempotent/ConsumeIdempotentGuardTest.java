package lan.chaos.idempotent;

import lan.chaos.idempotent.common.util.SampleFactory;
import lan.chaos.idempotent.core.ConsumeIdempotentGuard;
import lan.chaos.idempotent.core.H2IdempotencyStore;
import lan.chaos.idempotent.core.IdempotencyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 消费级幂等断言：同 messageId 的重复投递只被消费一次。
 */
class ConsumeIdempotentGuardTest {

    private ConsumeIdempotentGuard guard;

    @BeforeEach
    void setUp() {
        DataSource ds = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2).build();
        IdempotencyStore store = new H2IdempotencyStore(new JdbcTemplate(ds));
        guard = new ConsumeIdempotentGuard(store);
    }

    @Test
    void sameMessageId_consumedOnlyOnce() {
        String messageId = SampleFactory.newMessageId();
        String bizNo = SampleFactory.newBizNo();
        AtomicInteger handled = new AtomicInteger(0);

        guard.consume(messageId, bizNo, mid -> handled.incrementAndGet());
        guard.consume(messageId, bizNo, mid -> handled.incrementAndGet()); // 重试

        assertEquals(1, handled.get(), "同 messageId 重试应只处理一次");
        assertEquals(1, guard.consumedCount());
    }
}
