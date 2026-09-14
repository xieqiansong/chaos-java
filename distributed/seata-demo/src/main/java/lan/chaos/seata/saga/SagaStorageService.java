package lan.chaos.seata.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SAGA 库存参与者 — 正向扣减 + 逆向补偿。
 *
 * @author chaos
 */
@Service
public class SagaStorageService {

    private static final Logger log = LoggerFactory.getLogger(SagaStorageService.class);

    private final JdbcTemplate jdbcTemplate;

    public SagaStorageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 正向步骤：扣减库存（独立本地事务，CAS 防超卖）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String productId, int count) {
        List<Integer> totals = jdbcTemplate.query(
                "SELECT total FROM storage WHERE product_id = ?",
                (rs, rowNum) -> rs.getInt("total"),
                productId);
        if (totals.isEmpty()) {
            throw new RuntimeException("商品不存在: " + productId);
        }
        int rows = jdbcTemplate.update(
                "UPDATE storage SET total = total - ? WHERE product_id = ? AND total >= ?",
                count, productId, count);
        if (rows == 0) {
            throw new RuntimeException("库存不足: " + productId);
        }
        log.info("[SAGA-storage] 正向扣库存: productId={}, count={}", productId, count);
    }

    /**
     * 逆向补偿：加回库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void compensateDeduct(String productId, int count) {
        jdbcTemplate.update("UPDATE storage SET total = total + ? WHERE product_id = ?", count, productId);
        log.info("[SAGA-storage] 补偿加回: productId={}, count={}", productId, count);
    }
}
