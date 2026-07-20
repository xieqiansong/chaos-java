package lan.chaos.seata;

import lan.chaos.seata.at.AccountService;
import lan.chaos.seata.at.BusinessService;
import lan.chaos.seata.at.OrderService;
import lan.chaos.seata.at.StorageService;
import lan.chaos.seata.tcc.AccountTccAction;
import lan.chaos.seata.tcc.BusinessTccService;
import lan.chaos.seata.tcc.OrderTccAction;
import lan.chaos.seata.tcc.StorageTccAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static lan.chaos.seata.common.constant.SeataConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Seata 分布式事务场景测试。
 *
 * <p>测试策略：
 * 单测环境下 {@code seata.enabled=false}，不连接 Seata Server TC，
 * 因此 @GlobalTransactional 不退化为全局事务协调、TCC Confirm/Cancel 不会由框架自动触发。
 * 完整分布式行为需通过 docker-compose 启动 Seata Server 后验证（见 README）。
 *
 * <p>本测试聚焦：
 * <ol>
 *   <li>AT 模式：验证各服务 @Transactional 本地事务行为与业务逻辑正确性</li>
 *   <li>TCC 模式：验证 Try 阶段预留逻辑正确性</li>
 *   <li>异常传播：验证异常类型与消息</li>
 * </ol>
 *
 * <p>提交/取消逻辑（TCC Confirm/Cancel）在 commit/rollback 方法中实现，
 * 运行时由 Seata TC 框架回调触发；null-safe 防御保证单测不因缺少 context 而崩溃。</p>
 *
 * @author chaos
 */
@SpringBootTest
@DisplayName("Seata 分布式事务场景测试")
class SeataScenarioTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BusinessService businessService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private StorageService storageService;

    @Autowired
    private BusinessTccService businessTccService;
    @Autowired
    private AccountTccAction accountTccAction;
    @Autowired
    private OrderTccAction orderTccAction;
    @Autowired
    private StorageTccAction storageTccAction;

    // ==================== 辅助方法 ====================

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM order_tbl");
        jdbcTemplate.update("UPDATE account SET balance = ?, frozen = 0 WHERE user_id = ?", INIT_BALANCE, USER_ID);
        jdbcTemplate.update("MERGE INTO account (user_id, balance, frozen) KEY(user_id) VALUES (?, ?, 0)",
                USER_ID, INIT_BALANCE);
        jdbcTemplate.update("UPDATE storage SET total = ?, frozen = 0 WHERE product_id = ?", INIT_STOCK, PRODUCT_ID);
        jdbcTemplate.update("MERGE INTO storage (product_id, total, frozen) KEY(product_id) VALUES (?, ?, 0)",
                PRODUCT_ID, INIT_STOCK);
    }

    // ==================== AT 模式 ====================

    @Test
    @DisplayName("AT-1: 正常购买，数据一致性校验")
    void atPurchaseSuccess() {
        assertDoesNotThrow(() ->
                businessService.purchase(USER_ID, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT));

        assertEquals(INIT_BALANCE - ORDER_AMOUNT, accountService.getBalance(USER_ID), 0.01,
                "余额应减少 ORDER_AMOUNT");
        assertEquals(INIT_STOCK - ORDER_COUNT, storageService.getTotal(PRODUCT_ID),
                "库存应减少 ORDER_COUNT");
        assertEquals(1, orderService.count(), "应创建一条订单");
    }

    @Test
    @DisplayName("AT-2: 不存在的用户购买，应抛异常且无副作用")
    void atPurchaseFailUserNotFound() {
        double balanceBefore = accountService.getBalance(USER_ID);
        int stockBefore = storageService.getTotal(PRODUCT_ID);
        int ordersBefore = orderService.count();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                businessService.purchaseFail(USER_INSUFFICIENT, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT));
        assertTrue(ex.getMessage().contains("账户不存在"), "异常消息应包含'账户不存在'");

        assertEquals(balanceBefore, accountService.getBalance(USER_ID), 0.01, "余额不应变化");
        assertEquals(stockBefore, storageService.getTotal(PRODUCT_ID), "库存不应变化");
    }

    @Test
    @DisplayName("AT-3: 库存不足时应抛异常")
    void atPurchaseFailInsufficientStock() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                businessService.purchase(USER_ID, PRODUCT_ID, ORDER_AMOUNT, INIT_STOCK + 999));
        assertTrue(ex.getMessage().contains("库存不足"), "异常消息应包含'库存不足'");
    }

    @Test
    @DisplayName("AT-4: AccountService 本地事务回滚")
    void atAccountServiceRollback() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                accountService.deduct(USER_INSUFFICIENT, ORDER_AMOUNT));

        assertTrue(ex.getMessage().contains("账户不存在"));
        assertEquals(INIT_BALANCE, accountService.getBalance(USER_ID), 0.01,
                "正常用户余额不应受异常影响");
    }

    // ==================== TCC 模式 ====================

    @Test
    @DisplayName("TCC-1: Try 阶段冻结资源正确性")
    void tccPrepareFrozenState() {
        double balanceBefore = accountService.getBalance(USER_ID);

        // Try 冻结账户和库存
        assertDoesNotThrow(() -> accountTccAction.prepare(USER_ID, ORDER_AMOUNT));
        assertDoesNotThrow(() -> orderTccAction.prepare(USER_ID, PRODUCT_ID, ORDER_AMOUNT));
        assertDoesNotThrow(() -> storageTccAction.prepare(PRODUCT_ID, ORDER_COUNT));

        // 余额不应变化（Try 只冻结，不扣款）
        assertEquals(balanceBefore, accountService.getBalance(USER_ID), 0.01,
                "Try 阶段余额不变（只冻结）");
        // 冻结金额应增加
        Double frozenAccount = jdbcTemplate.queryForObject(
                "SELECT frozen FROM account WHERE user_id = ?", Double.class, USER_ID);
        assertEquals(ORDER_AMOUNT, frozenAccount, 0.01, "frozen 应为 ORDER_AMOUNT");

        // 订单 status=0（预留状态）
        Integer orderStatus = jdbcTemplate.queryForObject(
                "SELECT MIN(status) FROM order_tbl WHERE user_id = ?", Integer.class, USER_ID);
        assertEquals(0, orderStatus, "订单应为预留状态(status=0)");

        // 库存 frozen 增加
        Integer frozenStorage = jdbcTemplate.queryForObject(
                "SELECT frozen FROM storage WHERE product_id = ?", Integer.class, PRODUCT_ID);
        assertEquals(ORDER_COUNT, frozenStorage, "库存 frozen 应增加");
    }

    @Test
    @DisplayName("TCC-2: 余额不足时 Try 应抛异常")
    void tccPrepareInsufficientBalance() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                accountTccAction.prepare(USER_ID, INIT_BALANCE + 1));
        assertTrue(ex.getMessage().contains("余额不足"), "余额不足应抛异常");
    }

    @Test
    @DisplayName("TCC-3: Try 后 frozen 状态隔离：不影响余额查询")
    void tccFrozenIsolatedFromBalance() {
        accountTccAction.prepare(USER_ID, ORDER_AMOUNT);

        // balance 不变（冻结不影响可用余额查询）
        // 注：真实 TCC 场景下，业务应读取 "balance - frozen" 作为可用余额
        assertEquals(INIT_BALANCE, accountService.getBalance(USER_ID), 0.01,
                "Try 冻结后 balance 不变");
        // frozen > 0 表明有冻结资源
        Double frozen = jdbcTemplate.queryForObject(
                "SELECT frozen FROM account WHERE user_id = ?", Double.class, USER_ID);
        assertTrue(frozen > 0, "frozen 应大于 0");
    }

    @Test
    @DisplayName("TCC-4: BusinessTccService purchaseTry 正常流程")
    void tccBusinessPurchaseSuccess() {
        assertDoesNotThrow(() ->
                businessTccService.purchase(USER_ID, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT));

        Double frozen = jdbcTemplate.queryForObject(
                "SELECT frozen FROM account WHERE user_id = ?", Double.class, USER_ID);
        assertEquals(ORDER_AMOUNT, frozen, 0.01, "Try 后 frozen 应为 ORDER_AMOUNT");
    }

    @Test
    @DisplayName("TCC-5: BusinessTccService purchaseFail 应抛异常")
    void tccBusinessPurchaseFail() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                businessTccService.purchaseFail(USER_INSUFFICIENT, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT));
        assertTrue(ex.getMessage().contains("账户不存在"), "失败场景应抛异常");
    }
}
