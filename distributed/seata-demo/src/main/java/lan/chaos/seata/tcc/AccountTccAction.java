package lan.chaos.seata.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import static lan.chaos.seata.common.constant.SeataConstants.TCC_ACTION_ACCOUNT;

/**
 * 账户 TCC 资源 — Try/Confirm/Cancel 模式。
 *
 * <h3>TCC vs AT 的核心区别</h3>
 * <table>
 *   <tr><th>维度</th><th>AT（自动）</th><th>TCC（手动）</th></tr>
 *   <tr><td>回滚机制</td><td>Seata 自动记录 undo_log 反向补偿</td><td>开发者手写 Confirm / Cancel</td></tr>
 *   <tr><td>性能</td><td>有全局行锁（一阶段即锁定）</td><td>无全局锁，Try 只预留不锁定</td></tr>
 *   <tr><td>侵入性</td><td>几乎无侵入</td><td>需要拆 Try/Confirm/Cancel 三步</td></tr>
 *   <tr><td>适用场景</td><td>大部分 CRUD 场景</td><td>高性能场景、跨公司/跨系统对接</td></tr>
 * </table>
 *
 * <h3>TCC 三步曲（账户扣款）</h3>
 * <ol>
 *   <li><b>Try</b>：检查余额 → 冻结金额（frozen += amount），余额不变</li>
 *   <li><b>Confirm</b>：余额扣减 + 解冻（balance -= amount, frozen -= amount）</li>
 *   <li><b>Cancel</b>：解冻（frozen -= amount），余额恢复可用</li>
 * </ol>
 *
 * <p>为什么 Try 不直接扣余额？
 * 如果 Confirm 失败需要 Cancel，直接扣了再补回去容易产生中间状态。
 * "预留（冻结）→ 确认扣减 → 取消解冻"的模型更清晰。</p>
 *
 * <p>为什么 Confirm / Cancel 要幂等？
 * Seata TC 可能因网络超时重试 Confirm/Cancel，方法必须可重复执行而不产生副作用。
 * 这里用 frozen 字段做幂等：Cancel 时如果 frozen 已是 0 说明已解冻过。</p>
 *
 * @author chaos
 */
@Service
@LocalTCC
public class AccountTccAction {

    private static final Logger log = LoggerFactory.getLogger(AccountTccAction.class);

    private final JdbcTemplate jdbcTemplate;

    public AccountTccAction(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Try 阶段：冻结指定金额。
     * <p>使用 {@code BusinessActionContextParameter} 声明需传递到 commit/rollback 方法的参数。</p>
     *
     * @param userId 用户 ID
     * @param amount 冻结金额
     */
    @TwoPhaseBusinessAction(name = TCC_ACTION_ACCOUNT, commitMethod = "commit", rollbackMethod = "rollback")
    public boolean prepare(@BusinessActionContextParameter(paramName = "userId") String userId,
                           @BusinessActionContextParameter(paramName = "amount") double amount) {
        log.info("[TCC-account] Try: 冻结金额 userId={}, amount={}", userId, amount);

        // 检查余额是否足够（用 query + list 而非 queryForObject，
        // 后者在查不到结果时会抛 EmptyResultDataAccessException，不便区分"不存在"与"余额不足"）
        java.util.List<Double> balances = jdbcTemplate.query(
                "SELECT balance FROM account WHERE user_id = ?",
                (rs, rowNum) -> rs.getDouble("balance"),
                userId);
        if (balances.isEmpty()) {
            throw new RuntimeException("账户不存在: " + userId);
        }
        double balance = balances.get(0);
        if (balance < amount) {
            throw new RuntimeException(String.format("余额不足: userId=%s, balance=%.2f, need=%.2f", userId, balance, amount));
        }
        // 冻结：余额不变，frozen 增加
        jdbcTemplate.update("UPDATE account SET frozen = frozen + ? WHERE user_id = ?", amount, userId);
        return true;
    }

    /**
     * Confirm 阶段：解冻并真正扣款。
     * <p>必须幂等，TC 可能重试。</p>
     */
    public boolean commit(BusinessActionContext context) {
        String userId = (String) context.getActionContext("userId");
        double amount = Double.parseDouble(context.getActionContext("amount").toString());
        log.info("[TCC-account] Confirm: 确认扣款 userId={}, amount={}", userId, amount);

        // 幂等：只有当 frozen >= amount 时才执行（防止重复 Confirm）
        int rows = jdbcTemplate.update(
                "UPDATE account SET balance = balance - ?, frozen = frozen - ? WHERE user_id = ? AND frozen >= ?",
                amount, amount, userId, amount);
        if (rows == 0) {
            log.warn("[TCC-account] Confirm 幂等跳过: frozen 不足，已执行过");
        }
        return true;
    }

    /**
     * Cancel 阶段：解冻，恢复余额可用。
     * <p>必须幂等，TC 可能重试。</p>
     */
    public boolean rollback(BusinessActionContext context) {
        String userId = (String) context.getActionContext("userId");
        double amount = Double.parseDouble(context.getActionContext("amount").toString());
        log.info("[TCC-account] Cancel: 解冻 userId={}, amount={}", userId, amount);

        // 幂等：只有当 frozen >= amount 时才解冻
        int rows = jdbcTemplate.update(
                "UPDATE account SET frozen = frozen - ? WHERE user_id = ? AND frozen >= ?",
                amount, userId, amount);
        if (rows == 0) {
            log.warn("[TCC-account] Cancel 幂等跳过: frozen 不足，已执行过");
        }
        return true;
    }
}
