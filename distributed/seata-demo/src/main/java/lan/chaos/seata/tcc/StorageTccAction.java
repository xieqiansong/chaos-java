package lan.chaos.seata.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import static lan.chaos.seata.common.constant.SeataConstants.TCC_ACTION_STORAGE;

/**
 * 库存 TCC 资源。
 *
 * <h3>TCC 三步曲（库存扣减）</h3>
 * <ol>
 *   <li><b>Try</b>：检查库存 → 冻结库存（frozen += count）</li>
 *   <li><b>Confirm</b>：真正扣减（total -= count, frozen -= count）</li>
 *   <li><b>Cancel</b>：解冻（frozen -= count）</li>
 * </ol>
 *
 * <p>为什么 Try 要冻结：
 * 如果在 Try 直接扣 total，Confirm 前其他事务就能读到扣后的值。
 * 冻结模式让"已冻结但未确认"的库存不可被其他事务抢占，等 Confirm 再最终扣减。</p>
 *
 * @author chaos
 */
@Service
@LocalTCC
public class StorageTccAction {

    private static final Logger log = LoggerFactory.getLogger(StorageTccAction.class);

    private final JdbcTemplate jdbcTemplate;

    public StorageTccAction(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Try 阶段：冻结库存（total 不变，frozen 增加）。
     */
    @TwoPhaseBusinessAction(name = TCC_ACTION_STORAGE, commitMethod = "commit", rollbackMethod = "rollback")
    public boolean prepare(@BusinessActionContextParameter(paramName = "productId") String productId,
                           @BusinessActionContextParameter(paramName = "count") int count) {
        log.info("[TCC-storage] Try: 冻结库存 productId={}, count={}", productId, count);

        // 检查可用库存（total - frozen）
        // 用 query 代替 queryForObject：后者查不到结果会抛 EmptyResultDataAccessException
        java.util.List<Integer> availables = jdbcTemplate.query(
                "SELECT total - frozen FROM storage WHERE product_id = ?",
                (rs, rowNum) -> rs.getInt(1),
                productId);
        if (availables.isEmpty()) {
            throw new RuntimeException("商品不存在: " + productId);
        }
        int available = availables.get(0);
        if (available < count) {
            throw new RuntimeException(String.format("库存不足: productId=%s, available=%d, need=%d", productId, available, count));
        }
        jdbcTemplate.update("UPDATE storage SET frozen = frozen + ? WHERE product_id = ?", count, productId);
        return true;
    }

    /**
     * Confirm 阶段：真正扣减库存（frozen 和 total 同时减少）。
     * <p>必须幂等：WHERE frozen >= count。</p>
     */
    public boolean commit(BusinessActionContext context) {
        String productId = (String) context.getActionContext("productId");
        int count = Integer.parseInt(context.getActionContext("count").toString());
        log.info("[TCC-storage] Confirm: 确认扣减 productId={}, count={}", productId, count);

        int rows = jdbcTemplate.update(
                "UPDATE storage SET total = total - ?, frozen = frozen - ? WHERE product_id = ? AND frozen >= ?",
                count, count, productId, count);
        if (rows == 0) {
            log.warn("[TCC-storage] Confirm 幂等跳过: frozen 不足，已执行过");
        }
        return true;
    }

    /**
     * Cancel 阶段：解冻库存。
     * <p>必须幂等：WHERE frozen >= count。</p>
     */
    public boolean rollback(BusinessActionContext context) {
        String productId = (String) context.getActionContext("productId");
        int count = Integer.parseInt(context.getActionContext("count").toString());
        log.info("[TCC-storage] Cancel: 解冻 productId={}, count={}", productId, count);

        int rows = jdbcTemplate.update(
                "UPDATE storage SET frozen = frozen - ? WHERE product_id = ? AND frozen >= ?",
                count, productId, count);
        if (rows == 0) {
            log.warn("[TCC-storage] Cancel 幂等跳过: frozen 不足，已执行过");
        }
        return true;
    }
}
