package lan.chaos.seata.at;

import lan.chaos.seata.common.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static lan.chaos.seata.common.constant.SeataConstants.ORDER_AMOUNT;

/**
 * 账户服务 — AT 模式。
 *
 * <p>职责：从用户账户扣减余额。</p>
 *
 * <h3>AT 模式原理</h3>
 * <ol>
 *   <li>Seata 拦截当前 DataSource，业务 SQL 执行前自动 SELECT 记录"前镜像"</li>
 *   <li>业务 SQL 执行后再次 SELECT 记录"后镜像"</li>
 *   <li>若全局事务回滚，Seata 用 undo_log 反向生成补偿语句还原数据</li>
 * </ol>
 *
 * <p>因此你只需写业务 SQL，<b>不需要手动写回滚 SQL</b>，Seata 全自动。</p>
 *
 * <h3>生产化考量</h3>
 * <ul>
 *   <li>余额字段用 DECIMAL 而非 FLOAT/DOUBLE，避免精度问题</li>
 *   <li>高并发下扣款建议用 UPDATE ... SET balance = balance - ? WHERE balance >= ? 的 CAS 写法</li>
 *   <li>undo_log 表需定期清理，生产环境按天分区 + 定时任务删除 7 天前数据</li>
 * </ul>
 *
 * @author chaos
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final JdbcTemplate jdbcTemplate;

    public AccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 扣减账户余额。
     *
     * @param userId 用户 ID
     * @param amount 扣减金额
     * @throws RuntimeException 余额不足时抛出，触发全局事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String userId, double amount) {
        Account account = getAccount(userId);
        if (account == null) {
            throw new RuntimeException("账户不存在: " + userId);
        }
        if (account.getBalance() < amount) {
            throw new RuntimeException(String.format(
                    "余额不足: userId=%s, balance=%.2f, need=%.2f", userId, account.getBalance(), amount));
        }
        jdbcTemplate.update("UPDATE account SET balance = balance - ? WHERE user_id = ?", amount, userId);
        log.info("[AT] 账户扣款成功: userId={}, amount={}, balance={} -> {}",
                userId, amount, account.getBalance(), account.getBalance() - amount);
    }

    Account getAccount(String userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, balance, frozen FROM account WHERE user_id = ?",
                (rs, rowNum) -> new Account(
                        rs.getLong("id"),
                        rs.getString("user_id"),
                        rs.getDouble("balance"),
                        rs.getDouble("frozen")
                ),
                userId)
                .stream().findFirst().orElse(null);
    }

    /**
     * 查询余额（只读，不参与分布式事务写操作）。
     */
    public double getBalance(String userId) {
        Account a = getAccount(userId);
        return a != null ? a.getBalance() : 0;
    }
}
