package lan.chaos.seata;

import io.seata.rm.tcc.api.BusinessActionContext;
import lan.chaos.seata.tcc.AccountTccAction;
import lan.chaos.seata.tcc.OrderTccAction;
import lan.chaos.seata.tcc.StorageTccAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static lan.chaos.seata.common.constant.SeataConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TCC 二阶段语义测试：空回滚 / 幂等 / Confirm / Cancel。
 *
 * <p>单测环境 seata.enabled=false，TC 不会自动回调 Confirm/Cancel，
 * 因此这里直接手工构造 {@link BusinessActionContext} 调用二阶段方法，
 * 验证 TCC 三大经典问题（空回滚、幂等、悬挂）的实现是否正确。</p>
 *
 * <ul>
 *   <li><b>空回滚（Empty Rollback）</b>：Try 因网络超时未执行，但 TC 触发了 Cancel</li>
 *   <li><b>幂等（Idempotency）</b>：Confirm/Cancel 因网络重试被重复调用，不得产生副作用</li>
 *   <li><b>悬挂（Suspending）</b>：Confirm 之后迟到的 Cancel 不得破坏已确认的数据</li>
 * </ul>
 *
 * @author chaos
 */
@SpringBootTest
@DisplayName("TCC 二阶段语义测试（空回滚 / 幂等）")
class TccSemanticsTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AccountTccAction accountTccAction;
    @Autowired
    private OrderTccAction orderTccAction;
    @Autowired
    private StorageTccAction storageTccAction;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM order_tbl");
        jdbcTemplate.update("MERGE INTO account (user_id, balance, frozen) KEY(user_id) VALUES (?, ?, 0)",
                USER_ID, INIT_BALANCE);
        jdbcTemplate.update("MERGE INTO storage (product_id, total, frozen) KEY(product_id) VALUES (?, ?, 0)",
                PRODUCT_ID, INIT_STOCK);
    }

    /** 手工构造二阶段回调上下文（模拟 TC 回调时传入的 actionContext）。 */
    private BusinessActionContext context(String actionName, String... kv) {
        Map<String, Object> actionContext = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            actionContext.put(kv[i], kv[i + 1]);
        }
        return new BusinessActionContext("xid-" + actionName, "1", actionContext);
    }

    // ==================== 空回滚（Empty Rollback）====================

    @Test
    @DisplayName("TCC-语义-1: 空回滚 — Try 未执行直接 Cancel，不报错且无副作用")
    void emptyRollbackIsSafe() {
        // 场景：Try 因网络超时未执行，但 TC 触发了 Cancel（此时 frozen=0）
        BusinessActionContext accountCtx = context("account-deduct",
                "userId", USER_ID, "amount", String.valueOf(ORDER_AMOUNT));
        assertDoesNotThrow(() -> accountTccAction.rollback(accountCtx));
        assertEquals(0.0, accountFrozen(), 0.01, "空回滚后 frozen 仍为 0");
        assertEquals(INIT_BALANCE, accountBalance(), 0.01, "空回滚不影响余额");

        BusinessActionContext storageCtx = context("storage-deduct",
                "productId", PRODUCT_ID, "count", String.valueOf(ORDER_COUNT));
        assertDoesNotThrow(() -> storageTccAction.rollback(storageCtx));
        assertEquals(0, storageFrozen(), "空回滚后库存 frozen 仍为 0");

        // 订单空回滚：无 status=0 的预留订单可取消，不报错
        BusinessActionContext orderCtx = context("order-create",
                "userId", USER_ID, "productId", PRODUCT_ID, "amount", String.valueOf(ORDER_AMOUNT));
        assertDoesNotThrow(() -> orderTccAction.rollback(orderCtx));
        assertEquals(0, orderCount(), "空回滚不产生订单");
    }

    // ==================== 幂等（Idempotency）====================

    @Test
    @DisplayName("TCC-语义-2: 账户 Confirm 幂等 — 重复 Confirm 只扣一次")
    void accountConfirmIdempotent() {
        accountTccAction.prepare(USER_ID, ORDER_AMOUNT);
        BusinessActionContext ctx = context("account-deduct",
                "userId", USER_ID, "amount", String.valueOf(ORDER_AMOUNT));

        accountTccAction.commit(ctx);
        assertEquals(INIT_BALANCE - ORDER_AMOUNT, accountBalance(), 0.01, "第一次 Confirm 扣款");
        assertEquals(0.0, accountFrozen(), 0.01, "Confirm 后冻结归零");

        accountTccAction.commit(ctx);
        assertEquals(INIT_BALANCE - ORDER_AMOUNT, accountBalance(), 0.01, "第二次 Confirm 幂等跳过，不再扣款");
        assertEquals(0.0, accountFrozen(), 0.01);
    }

    @Test
    @DisplayName("TCC-语义-3: 账户 Cancel 幂等 — 重复 Cancel 只解冻一次")
    void accountCancelIdempotent() {
        accountTccAction.prepare(USER_ID, ORDER_AMOUNT);
        BusinessActionContext ctx = context("account-deduct",
                "userId", USER_ID, "amount", String.valueOf(ORDER_AMOUNT));

        accountTccAction.rollback(ctx);
        assertEquals(INIT_BALANCE, accountBalance(), 0.01, "Cancel 只解冻不扣款");
        assertEquals(0.0, accountFrozen(), 0.01, "Cancel 后冻结归零");

        accountTccAction.rollback(ctx);
        assertEquals(INIT_BALANCE, accountBalance(), 0.01, "重复 Cancel 无副作用");
        assertEquals(0.0, accountFrozen(), 0.01);
    }

    // ==================== 悬挂（Confirm 后迟到 Cancel）====================

    @Test
    @DisplayName("TCC-语义-4: 悬挂防护 — Confirm 之后迟到的 Cancel 不得破坏已确认数据")
    void confirmThenLateCancelNoSideEffect() {
        // 账户：Confirm 扣款后再触发 Cancel，不应反向加回余额
        accountTccAction.prepare(USER_ID, ORDER_AMOUNT);
        BusinessActionContext accountCtx = context("account-deduct",
                "userId", USER_ID, "amount", String.valueOf(ORDER_AMOUNT));
        accountTccAction.commit(accountCtx);
        accountTccAction.rollback(accountCtx); // 迟到的 Cancel：frozen=0，幂等跳过
        assertEquals(INIT_BALANCE - ORDER_AMOUNT, accountBalance(), 0.01, "迟到 Cancel 不应影响已确认扣款");
        assertEquals(0.0, accountFrozen(), 0.01);

        // 库存：Confirm 扣减后再 Cancel，不应补回已确认扣减
        storageTccAction.prepare(PRODUCT_ID, ORDER_COUNT);
        BusinessActionContext storageCtx = context("storage-deduct",
                "productId", PRODUCT_ID, "count", String.valueOf(ORDER_COUNT));
        storageTccAction.commit(storageCtx);
        storageTccAction.rollback(storageCtx);
        assertEquals(INIT_STOCK - ORDER_COUNT, storageTotal(), "迟到 Cancel 不影响已确认扣减");
        assertEquals(0, storageFrozen());
    }

    // ==================== 订单 / 库存状态流转 ====================

    @Test
    @DisplayName("TCC-语义-5: 订单 Confirm/Cancel 状态流转 0→1 / 0→-1，且幂等")
    void orderStatusTransitionIdempotent() {
        BusinessActionContext ctx = context("order-create",
                "userId", USER_ID, "productId", PRODUCT_ID, "amount", String.valueOf(ORDER_AMOUNT));

        // 确认路径：预留(0) → 完成(1)
        orderTccAction.prepare(USER_ID, PRODUCT_ID, ORDER_AMOUNT);
        orderTccAction.commit(ctx);
        assertEquals(1, countOrdersByStatus(1), "Confirm 后存在 status=1 订单");
        orderTccAction.commit(ctx);
        assertEquals(1, countOrdersByStatus(1), "重复 Confirm 幂等，不重复置为完成");

        // 取消路径：预留(0) → 取消(-1)
        orderTccAction.prepare(USER_ID, PRODUCT_ID, ORDER_AMOUNT);
        orderTccAction.rollback(ctx);
        assertEquals(1, countOrdersByStatus(-1), "Cancel 后存在 status=-1 订单");
        orderTccAction.rollback(ctx);
        assertEquals(1, countOrdersByStatus(-1), "重复 Cancel 幂等");
    }

    @Test
    @DisplayName("TCC-语义-6: 库存 Confirm/Cancel 冻结与扣减正确且幂等")
    void storageConfirmCancelIdempotent() {
        BusinessActionContext ctx = context("storage-deduct",
                "productId", PRODUCT_ID, "count", String.valueOf(ORDER_COUNT));

        // Confirm：total 扣减、frozen 归零
        storageTccAction.prepare(PRODUCT_ID, ORDER_COUNT);
        storageTccAction.commit(ctx);
        assertEquals(INIT_STOCK - ORDER_COUNT, storageTotal(), "Confirm 扣减 total");
        assertEquals(0, storageFrozen(), "Confirm 后冻结归零");
        storageTccAction.commit(ctx);
        assertEquals(INIT_STOCK - ORDER_COUNT, storageTotal(), "重复 Confirm 幂等");

        // Cancel：total 保持 Confirm 后的值（99，不扣也不恢复）、frozen 归零
        storageTccAction.prepare(PRODUCT_ID, ORDER_COUNT);
        assertEquals(INIT_STOCK - ORDER_COUNT, storageTotal(), "Cancel 阶段 prepare 不扣 total");
        storageTccAction.rollback(ctx);
        assertEquals(INIT_STOCK - ORDER_COUNT, storageTotal(), "Cancel 不扣 total，仍为 Confirm 后的值");
        assertEquals(0, storageFrozen(), "Cancel 后冻结归零");
        storageTccAction.rollback(ctx);
        assertEquals(INIT_STOCK - ORDER_COUNT, storageTotal(), "重复 Cancel 幂等");
    }

    // ==================== 辅助查询 ====================

    private double accountBalance() {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?", Double.class, USER_ID);
    }

    private double accountFrozen() {
        return jdbcTemplate.queryForObject(
                "SELECT frozen FROM account WHERE user_id = ?", Double.class, USER_ID);
    }

    private int storageTotal() {
        return jdbcTemplate.queryForObject(
                "SELECT total FROM storage WHERE product_id = ?", Integer.class, PRODUCT_ID);
    }

    private int storageFrozen() {
        return jdbcTemplate.queryForObject(
                "SELECT frozen FROM storage WHERE product_id = ?", Integer.class, PRODUCT_ID);
    }

    private int orderCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_tbl", Integer.class);
    }

    private int countOrdersByStatus(int status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_tbl WHERE status = ?", Integer.class, status);
    }
}
