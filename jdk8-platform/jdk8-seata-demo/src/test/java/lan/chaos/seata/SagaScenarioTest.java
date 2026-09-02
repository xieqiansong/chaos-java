package lan.chaos.seata;

import lan.chaos.seata.saga.SagaBusinessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static lan.chaos.seata.common.constant.SeataConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SAGA 补偿链场景测试（纯本地，无需 Seata Server）。
 *
 * <p>验证 SAGA 的核心机制——补偿链：</p>
 * <ol>
 *   <li>正向全部成功：各参与者独立本地提交，数据一致</li>
 *   <li>首步失败：无副作用（无需补偿）</li>
 *   <li>中间步骤失败：对已成功步骤按逆序补偿（撤销订单 → 加回余额）</li>
 * </ol>
 *
 * @author chaos
 */
@SpringBootTest
@DisplayName("SAGA 补偿链场景测试")
class SagaScenarioTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SagaBusinessService sagaBusinessService;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM order_tbl");
        jdbcTemplate.update("MERGE INTO account (user_id, balance, frozen) KEY(user_id) VALUES (?, ?, 0)",
                USER_ID, INIT_BALANCE);
        jdbcTemplate.update("MERGE INTO storage (product_id, total, frozen) KEY(product_id) VALUES (?, ?, 0)",
                PRODUCT_ID, INIT_STOCK);
    }

    @Test
    @DisplayName("SAGA-1: 正向全部成功，数据一致")
    void sagaPurchaseSuccess() {
        String orderNo = sagaBusinessService.purchase(USER_ID, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT);

        assertNotNull(orderNo, "应返回订单号");
        assertEquals(INIT_BALANCE - ORDER_AMOUNT, balance(), 0.01, "余额扣减");
        assertEquals(INIT_STOCK - ORDER_COUNT, stock(), "库存扣减");
        assertEquals(1, orderCount(), "创建一条订单");
    }

    @Test
    @DisplayName("SAGA-2: 首步扣款失败，无副作用（无需补偿）")
    void sagaAccountFailNoSideEffect() {
        assertThrows(RuntimeException.class,
                () -> sagaBusinessService.purchase(USER_INSUFFICIENT, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT));

        assertEquals(INIT_BALANCE, balance(), 0.01, "余额不变");
        assertEquals(INIT_STOCK, stock(), "库存不变");
        assertEquals(0, orderCount(), "无订单");
    }

    @Test
    @DisplayName("SAGA-3: 扣库存失败触发补偿链 — 撤销订单 + 加回余额")
    void sagaInsufficientStockCompensatesChain() {
        // 正向：扣款成功 + 建单成功 + 扣库存失败
        // 逆序补偿：撤销订单 → 加回余额
        assertThrows(RuntimeException.class,
                () -> sagaBusinessService.purchase(USER_ID, PRODUCT_ID, ORDER_AMOUNT, INIT_STOCK + 999));

        assertEquals(INIT_BALANCE, balance(), 0.01, "补偿后余额恢复");
        assertEquals(INIT_STOCK, stock(), "库存不变（扣减失败）");
        assertEquals(0, orderCount(), "补偿后订单被撤销");
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
