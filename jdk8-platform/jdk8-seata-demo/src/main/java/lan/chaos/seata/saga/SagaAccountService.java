package lan.chaos.seata.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SAGA 账户参与者 — 正向扣款 + 逆向补偿。
 *
 * <p>与 AT/TCC 不同：SAGA 每个参与者是一个<b>独立本地事务</b>，执行完立即提交
 * （不持有全局锁、不写 undo_log）。一旦后续步骤失败，由编排者按<b>逆序</b>
 * 调用补偿方法恢复。</p>
 *
 * <p>为什么 SAGA 可以接受"先扣款后补偿"的中间状态：
 * SAGA 面向长事务（如跨月对账、物流流转），相比强一致更看重
 * 不长期持锁、单步可控、可人工干预。</p>
 *
 * @author chaos
 */
@Service
public class SagaAccountService {

    private static final Logger log = LoggerFactory.getLogger(SagaAccountService.class);

    private final JdbcTemplate jdbcTemplate;

    public SagaAccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 正向步骤：扣款（独立本地事务，执行完即提交）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String userId, double amount) {
        List<Double> balances = jdbcTemplate.query(
                "SELECT balance FROM account WHERE user_id = ?",
                (rs, rowNum) -> rs.getDouble("balance"),
                userId);
        if (balances.isEmpty()) {
            throw new RuntimeException("账户不存在: " + userId);
        }
        if (balances.get(0) < amount) {
            throw new RuntimeException("余额不足: " + userId);
        }
        // CAS 写法：防止并发下扣成负数
        int rows = jdbcTemplate.update(
                "UPDATE account SET balance = balance - ? WHERE user_id = ? AND balance >= ?",
                amount, userId, amount);
        if (rows == 0) {
            throw new RuntimeException("扣款失败（并发冲突）: " + userId);
        }
        log.info("[SAGA-account] 正向扣款: userId={}, amount={}", userId, amount);
    }

    /**
     * 逆向补偿：加回余额（SAGA 对"扣款"步骤的补偿语义）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void compensateDeduct(String userId, double amount) {
        jdbcTemplate.update("UPDATE account SET balance = balance + ? WHERE user_id = ?", amount, userId);
        log.info("[SAGA-account] 补偿加回: userId={}, amount={}", userId, amount);
    }
}
