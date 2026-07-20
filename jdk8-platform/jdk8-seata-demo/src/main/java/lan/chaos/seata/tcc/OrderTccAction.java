package lan.chaos.seata.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static lan.chaos.seata.common.constant.SeataConstants.TCC_ACTION_ORDER;

/**
 * 订单 TCC 资源。
 *
 * <h3>TCC 三步曲（订单创建）</h3>
 * <ol>
 *   <li><b>Try</b>：创建订单，status=0（创建中/预留）</li>
 *   <li><b>Confirm</b>：修改 status=1（已完成）</li>
 *   <li><b>Cancel</b>：修改 status=-1（已取消），或删除订单</li>
 * </ol>
 *
 * <p>为什么不用 DELETE 做 Cancel？
 * 保留取消记录便于对账，生产环境订单应走"流程关闭"而非物理删除。</p>
 *
 * @author chaos
 */
@Service
@LocalTCC
public class OrderTccAction {

    private static final Logger log = LoggerFactory.getLogger(OrderTccAction.class);

    private final JdbcTemplate jdbcTemplate;

    public OrderTccAction(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Try 阶段：创建预留订单（status=0）。
     * <p>@TwoPhaseBusinessAction 方法必须返回 boolean；
     * orderNo 通过 userId + status=0 在 Confirm/Cancel 中间接定位。</p>
     */
    @TwoPhaseBusinessAction(name = TCC_ACTION_ORDER, commitMethod = "commit", rollbackMethod = "rollback")
    public boolean prepare(@BusinessActionContextParameter(paramName = "userId") String userId,
                           @BusinessActionContextParameter(paramName = "productId") String productId,
                           @BusinessActionContextParameter(paramName = "amount") double amount) {
        String orderNo = UUID.randomUUID().toString().replace("-", "");
        log.info("[TCC-order] Try: 创建预留订单 orderNo={}, userId={}, amount={}", orderNo, userId, amount);

        jdbcTemplate.update(
                "INSERT INTO order_tbl (user_id, product_id, amount, status, order_no) VALUES (?, ?, ?, 0, ?)",
                userId, productId, amount, orderNo);

        log.info("[TCC-order] Try 完成: orderNo={}", orderNo);
        return true;
    }

    /**
     * Confirm 阶段：订单状态→已完成。
     * <p>必须幂等：WHERE status=0 确保只更新一次。</p>
     */
    public boolean commit(BusinessActionContext context) {
        // orderNo 需从方法返回值或额外字段取。这里通过查询 Try 阶段插入的订单来获取
        String userId = (String) context.getActionContext("userId");
        log.info("[TCC-order] Confirm: 确认订单 userId={}", userId);

        int rows = jdbcTemplate.update(
                "UPDATE order_tbl SET status = 1 WHERE user_id = ? AND status = 0", userId);
        if (rows == 0) {
            log.warn("[TCC-order] Confirm 幂等跳过: 无 status=0 的订单，已执行过");
        }
        return true;
    }

    /**
     * Cancel 阶段：订单状态→已取消。
     * <p>必须幂等：WHERE status=0 确保只更新一次。</p>
     */
    public boolean rollback(BusinessActionContext context) {
        String userId = (String) context.getActionContext("userId");
        log.info("[TCC-order] Cancel: 取消订单 userId={}", userId);

        int rows = jdbcTemplate.update(
                "UPDATE order_tbl SET status = -1 WHERE user_id = ? AND status = 0", userId);
        if (rows == 0) {
            log.warn("[TCC-order] Cancel 幂等跳过: 无 status=0 的订单，已执行过");
        }
        return true;
    }
}
