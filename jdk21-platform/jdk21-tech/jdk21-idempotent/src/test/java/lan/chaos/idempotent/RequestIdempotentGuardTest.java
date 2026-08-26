package lan.chaos.idempotent;

import lan.chaos.idempotent.common.model.BizOrder;
import lan.chaos.idempotent.common.util.SampleFactory;
import lan.chaos.idempotent.core.H2IdempotencyStore;
import lan.chaos.idempotent.core.IdempotencyStore;
import lan.chaos.idempotent.core.RequestIdempotentGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 请求级幂等断言：
 * 1. 同一 requestId 重复调用 → 副作用只执行一次（并发双发）。
 * 2. 重复调用返回「已处理」占位，不脏写。
 */
class RequestIdempotentGuardTest {

    private IdempotencyStore store;
    private RequestIdempotentGuard guard;

    @BeforeEach
    void setUp() {
        DataSource ds = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setScriptEncoding("UTF-8")
                .build();
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        store = new H2IdempotencyStore(jdbc);
        guard = new RequestIdempotentGuard(store);
    }

    @Test
    void sameRequestId_onlyOneSideEffect_evenUnderConcurrentDoubleSubmit() throws InterruptedException {
        String requestId = SampleFactory.newRequestId();
        String bizNo = SampleFactory.newBizNo();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        AtomicReference<BizOrder> r1 = new AtomicReference<>();
        AtomicReference<BizOrder> r2 = new AtomicReference<>();
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                BizOrder r = guard.execute(requestId, bizNo, () ->
                        BizOrder.builder().bizNo(bizNo).action("CREATE").state("CREATED").build());
                if (r1.get() == null) r1.set(r); else r2.set(r);
            });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);

        // 核心断言：并发双发下，真实副作用只执行一次
        assertEquals(1, guard.sideEffectCount(), "并发双发应只执行一次业务副作用");
        // 去重键已落表
        assertEquals(true, store.exists(requestId, "REQUEST"));
    }

    @Test
    void repeatedRequest_returnsAlreadyDone_placeholder() {
        String requestId = SampleFactory.newRequestId();
        String bizNo = SampleFactory.newBizNo();
        guard.execute(requestId, bizNo, () ->
                BizOrder.builder().bizNo(bizNo).action("CREATE").state("CREATED").build());
        BizOrder second = guard.execute(requestId, bizNo, () ->
                BizOrder.builder().bizNo(bizNo).action("CREATE").state("CREATED").build());

        assertFalse(second.getState().equals("CREATED"), "重复请求不应再触发 CREATE 副作用");
        assertEquals("ALREADY_DONE", second.getState());
        assertEquals(1, guard.sideEffectCount());
    }
}
