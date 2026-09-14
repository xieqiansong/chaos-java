package lan.chaos.seata;

import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lan.chaos.seata.at.AccountService;
import lan.chaos.seata.at.BusinessService;
import lan.chaos.seata.at.StorageService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import static lan.chaos.seata.common.constant.SeataConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AT undo_log 真实镜像集成测试（需要真实 Seata Server，否则 Assumptions 跳过）。
 *
 * <p>运行方式：</p>
 * <pre>
 *   docker compose -f jdk8-seata-demo/docker-compose.yml up -d
 *   mvn -pl jdk8-seata-demo -am test -Dseata.enabled=true
 * </pre>
 *
 * <p>通过手动开启全局事务（GlobalTransactionContext），在提交/回滚决策之前
 * 确定性地断言 undo_log 中已写入镜像记录；回滚后验证数据被恢复、镜像被清理。</p>
 *
 * @author chaos
 */
@SpringBootTest
@DisplayName("AT undo_log 真实镜像集成测试")
class AtUndoLogIntegrationTest {

    @Autowired
    private Environment environment;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private StorageService storageService;

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
    @DisplayName("AT-mirror-1: 分支事务执行后 undo_log 写入镜像，回滚后清理并恢复数据")
    void globalTxWritesMirrorAndRollbackRestores() throws Exception {
        // 手动开启全局事务：可在提交/回滚决策前确定性地观察 undo_log，
        // 避免 @GlobalTransactional 提交后异步清理造成的竞态。
        GlobalTransaction tx = GlobalTransactionContext.createNew();
        tx.begin();

        accountService.deduct(USER_ID, ORDER_AMOUNT);   // 分支 1：扣款，写 undo_log
        storageService.deduct(PRODUCT_ID, ORDER_COUNT); // 分支 2：扣库存，写 undo_log

        // 提交决策前：应有 2 条镜像记录，且 rollback_info 非空、log_status=0（待回滚）
        int mirrorRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM undo_log", Integer.class);
        assertEquals(2, mirrorRows, "两个分支事务应产生两条 undo_log 镜像");
        Integer nonEmpty = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM undo_log WHERE rollback_info IS NOT NULL AND log_status = 0",
                Integer.class);
        assertEquals(2, nonEmpty, "镜像应含 rollback_info 且 log_status=0");

        // 回滚：Seata 按 undo_log 前镜像反向补偿，数据恢复
        tx.rollback();
        assertEquals(INIT_BALANCE, accountService.getBalance(USER_ID), 0.01, "回滚后余额恢复");
        assertEquals(INIT_STOCK, storageService.getTotal(PRODUCT_ID), "回滚后库存恢复");

        Integer leftover = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM undo_log", Integer.class);
        assertEquals(0, leftover, "回滚完成后 undo_log 应被清理");
    }

    @Test
    @DisplayName("AT-mirror-2: @GlobalTransactional 全局失败自动回滚并恢复全部资源")
    void globalFailAutoRollbackRestoresAll() {
        assertThrows(RuntimeException.class,
                () -> businessService.purchaseFail(USER_INSUFFICIENT, PRODUCT_ID, ORDER_AMOUNT, ORDER_COUNT));

        assertEquals(INIT_BALANCE, accountService.getBalance(USER_ID), 0.01, "余额恢复");
        assertEquals(INIT_STOCK, storageService.getTotal(PRODUCT_ID), "库存恢复");
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_tbl", Integer.class), "订单无残留");
    }
}
