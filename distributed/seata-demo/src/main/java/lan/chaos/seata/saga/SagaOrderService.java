package lan.chaos.seata.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SAGA 订单参与者 — 正向建单 + 逆向撤销。
 *
 * <p>补偿语义：SAGA 下订单在事务未完成前不对客户可见，
 * 因此撤销用<b>物理删除</b>即可；若订单已对外可见（如已发货），
 * 则补偿应改为"状态置为已取消"而不是删除（生产化要点）。</p>
 *
 * @author chaos
 */
@Service
public class SagaOrderService {

    private static final Logger log = LoggerFactory.getLogger(SagaOrderService.class);

    private final JdbcTemplate jdbcTemplate;

    public SagaOrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 正向步骤：创建订单（status=1，独立本地事务）。
     *
     * @param orderNo 由编排者生成并透传，补偿时按它定位
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(String userId, String productId, double amount, String orderNo) {
        jdbcTemplate.update(
                "INSERT INTO order_tbl (user_id, product_id, amount, status, order_no) VALUES (?, ?, ?, 1, ?)",
                userId, productId, amount, orderNo);
        log.info("[SAGA-order] 正向建单: orderNo={}", orderNo);
    }

    /**
     * 逆向补偿：撤销订单（按 orderNo 定位，幂等——不存在时影响 0 行）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void compensateCreate(String orderNo) {
        int rows = jdbcTemplate.update("DELETE FROM order_tbl WHERE order_no = ?", orderNo);
        log.info("[SAGA-order] 补偿撤销: orderNo={}, 删除行数={}", orderNo, rows);
    }
}
