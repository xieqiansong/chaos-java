package lan.chaos.seata;

import lan.chaos.seata.at.BusinessService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static lan.chaos.seata.common.constant.SeataConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AT 全局锁并发测试（需要真实 Seata Server，否则 Assumptions 跳过）。
 *
 * <p>多个全局事务并发对同一用户/商品发起购买：Seata 对已修改的行持有<b>全局锁</b>，
 * 并发事务需等待锁释放（lock retry），配合 SQL 的 CAS 写法（WHERE total >= ?）
 * 保证不超卖、不脏写、最终数据精确一致。</p>
 *
 * @author chaos
 */
@SpringBootTest
@DisplayName("AT 全局锁并发测试")
class GlobalLockConcurrencyTest {

    @Autowired
    private Environment environment;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void requireRealSeataServer() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(environment.getProperty("seata.enabled", "false")),
                "需真实 Seata Server，跳过（docker-compose + -Dseata.enabled=true 时运行）");
    }

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM order_tbl");
        jdbcTemplate.update("DELETE FROM undo_log");
        jdbcTemplate.update("MERGE INTO account (user_id, balance, frozen) KEY(user_id) VALUES (?, ?, 0)",
                USER_ID, INIT_BALANCE);
        jdbcTemplate.update("MERGE INTO storage (product_id, total, frozen) KEY(product_id) VALUES (?, ?, 0)",
                PRODUCT_ID, INIT_STOCK);
    }

    @Test
    @DisplayName("AT-lock-1: 并发购买数据精确一致（全局锁 + CAS）")
    void concurrentPurchasesKeepConsistency() throws Exception {
        int threads = 8;
        int ordersPerThread = 5; // 共 40 单 < 库存 100，全部应成功

        PurchaseResult result = runConcurrentPurchases(threads, ordersPerThread);

        int expected = threads * ordersPerThread;
        assertEquals(expected, result.success, "库存/余额充足时应全部成功");
        assertEquals(0, result.failure, "不应有因锁冲突导致的失败");

        assertEquals(INIT_BALANCE - ORDER_AMOUNT * expected, balance(), 0.01, "余额精确扣减");
        assertEquals(INIT_STOCK - ORDER_COUNT * expected, stock(), "库存精确扣减，无超卖");
        assertEquals(expected, orderCount(), "订单数与成功数一致");
    }

    @Test
    @DisplayName("AT-lock-2: 库存不足时并发抢购不扣成负数（防超卖）")
    void concurrentOversellPrevented() throws Exception {
        int limitedStock = 3;
        jdbcTemplate.update("UPDATE storage SET total = ? WHERE product_id = ?", limitedStock, PRODUCT_ID);

        int threads = 10;
        PurchaseResult result = runConcurrentPurchases(threads, 1);

        assertTrue(stock() >= 0, "库存不得为负");
        assertEquals(limitedStock, result.success + stock(), "成功数 + 剩余库存 = 初始库存（无超卖）");
        assertTrue(result.failure > 0, "超出库存的请求应失败");
    }

    /** 并发执行购买，返回成功/失败计数。 */
    private PurchaseResult runConcurrentPurchases(int threads, int ordersPerThread) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                for (int j = 0; j < ordersPerThread; j++) {
                    try {
                        businessService.purchase(USER_ID, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT);
                        success.incrementAndGet();
                    } catch (RuntimeException e) {
                        failure.incrementAndGet();
                    }
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(120, TimeUnit.SECONDS);
        }
        pool.shutdown();

        return new PurchaseResult(success.get(), failure.get());
    }

    private static final class PurchaseResult {
        final int success;
        final int failure;

        PurchaseResult(int success, int failure) {
            this.success = success;
            this.failure = failure;
        }
    }

    private double balance() {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?", Double.class, USER_ID);
    }

    private int stock() {
        return jdbcTemplate.queryForObject(
                "SELECT total FROM storage WHERE product_id = ?", Integer.class, PRODUCT_ID);
    }

    private int orderCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_tbl", Integer.class);
    }
}
