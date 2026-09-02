package lan.chaos.seata;

import lan.chaos.seata.at.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static lan.chaos.seata.common.constant.SeataConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AT 模式 undo_log 结构测试（单测环境，seata.enabled=false）。
 *
 * <p>验证：</p>
 * <ol>
 *   <li>undo_log 表由 schema.sql 正确创建（AT 模式的基石表）</li>
 *   <li>未开启全局事务时，本地 @Transactional 不会产生 undo_log 镜像</li>
 * </ol>
 *
 * <p>真实镜像记录 / 回滚补偿需真实 Seata Server，见 AtUndoLogIntegrationTest。</p>
 *
 * @author chaos
 */
@SpringBootTest
@DisplayName("AT undo_log 结构测试")
class AtUndoLogTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AccountService accountService;

    @Test
    @DisplayName("AT-undo-1: undo_log 表存在")
    void undoLogTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'undo_log'",
                Integer.class);
        assertTrue(count != null && count > 0, "schema.sql 应创建 undo_log 表");
    }

    @Test
    @DisplayName("AT-undo-2: 本地事务不生成 undo_log 镜像")
    void localTxGeneratesNoUndoLog() {
        // 单测环境 seata.enabled=false：Seata 不拦截 DataSource，@Transactional 走本地事务
        accountService.deduct(USER_ID, ORDER_AMOUNT);
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM undo_log", Integer.class);
        assertEquals(0, rows, "未开启全局事务时不应产生 undo_log 记录");
    }
}
