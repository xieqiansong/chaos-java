package lan.chaos.seata.at;

import lan.chaos.seata.common.model.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存服务 — AT 模式。
 *
 * <p>职责：扣减商品库存。</p>
 *
 * <h3>为什么扣库存失败要抛异常</h3>
 * <p>AT 模式下，只有抛出 RuntimeException 才会触发全局回滚。
 * 如果返回错误码而不抛异常，Seata 无法感知失败，不会回滚前序操作。</p>
 *
 * <h3>生产化考量</h3>
 * <ul>
 *   <li>热点商品库存扣减：建议 Redis 做库存缓存 + Lua 原子扣减 + 异步落库</li>
 *   <li>超卖防护：SQL 层面 WHERE total >= ? 保证不会扣成负数</li>
 * </ul>
 *
 * @author chaos
 */
@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final JdbcTemplate jdbcTemplate;

    public StorageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 扣减库存。
     *
     * @param productId 商品 ID
     * @param count     扣减数量
     * @throws RuntimeException 库存不足时抛出，触发全局事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(String productId, int count) {
        Storage storage = getStorage(productId);
        if (storage == null) {
            throw new RuntimeException("商品不存在: " + productId);
        }
        if (storage.getTotal() < count) {
            throw new RuntimeException(String.format(
                    "库存不足: productId=%s, total=%d, need=%d", productId, storage.getTotal(), count));
        }
        // CAS 写法：UPDATE WHERE total >= ?，天然防超卖
        int rows = jdbcTemplate.update(
                "UPDATE storage SET total = total - ? WHERE product_id = ? AND total >= ?",
                count, productId, count);
        if (rows == 0) {
            throw new RuntimeException("库存扣减失败（并发冲突）: " + productId);
        }
        log.info("[AT] 库存扣减成功: productId={}, count={}, total={} -> {}",
                productId, count, storage.getTotal(), storage.getTotal() - count);
    }

    Storage getStorage(String productId) {
        return jdbcTemplate.query(
                "SELECT id, product_id, total, frozen FROM storage WHERE product_id = ?",
                (rs, rowNum) -> new Storage(
                        rs.getLong("id"),
                        rs.getString("product_id"),
                        rs.getInt("total"),
                        rs.getInt("frozen")
                ),
                productId)
                .stream().findFirst().orElse(null);
    }

    /**
     * 查询库存（测试断言用）。
     */
    public int getTotal(String productId) {
        Storage s = getStorage(productId);
        return s != null ? s.getTotal() : 0;
    }
}
