package lan.chaos.seata;

import lan.chaos.seata.at.AccountService;
import lan.chaos.seata.at.OrderService;
import lan.chaos.seata.at.StorageService;
import lan.chaos.seata.xa.XaPurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static lan.chaos.seata.common.constant.SeataConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * XA 模式业务场景测试。
 *
 * <p>单测环境 seata.enabled=false：验证 XA 模式业务代码（与 AT 完全一致）的本地正确性。
 * 真正的 XA 二阶段协调需：真实 Seata Server + DataSourceProxyXA 代理 + 支持 XA 的数据库
 * （见 README XA 章节与 common/config/XaDataSourceConfig）。</p>
 *
 * @author chaos
 */
@SpringBootTest
@DisplayName("XA 模式业务场景测试")
class XaScenarioTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private XaPurchaseService xaPurchaseService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private StorageService storageService;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM order_tbl");
        jdbcTemplate.update("MERGE INTO account (user_id, balance, frozen) KEY(user_id) VALUES (?, ?, 0)",
                USER_ID, INIT_BALANCE);
        jdbcTemplate.update("MERGE INTO storage (product_id, total, frozen) KEY(product_id) VALUES (?, ?, 0)",
                PRODUCT_ID, INIT_STOCK);
    }

    @Test
    @DisplayName("XA-1: 正常购买数据一致")
    void xaPurchaseSuccess() {
        assertDoesNotThrow(() ->
                xaPurchaseService.purchase(USER_ID, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT));

        assertEquals(INIT_BALANCE - ORDER_AMOUNT, accountService.getBalance(USER_ID), 0.01, "余额扣减");
        assertEquals(INIT_STOCK - ORDER_COUNT, storageService.getTotal(PRODUCT_ID), "库存扣减");
        assertEquals(1, orderService.count(), "创建一条订单");
    }

    @Test
    @DisplayName("XA-2: 失败场景无副作用")
    void xaPurchaseFailNoSideEffect() {
        assertThrows(RuntimeException.class, () ->
                xaPurchaseService.purchase(USER_INSUFFICIENT, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT));

        assertEquals(INIT_BALANCE, accountService.getBalance(USER_ID), 0.01, "余额不变");
        assertEquals(INIT_STOCK, storageService.getTotal(PRODUCT_ID), "库存不变");
        assertEquals(0, orderService.count(), "无订单");
    }
}
