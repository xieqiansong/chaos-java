package lan.chaos.seata.at;

import lan.chaos.seata.common.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 订单服务 — AT 模式。
 *
 * <p>职责：创建订单记录。</p>
 *
 * <p>为什么用 UUID 生成 orderNo：
 * 分布式环境下自增 ID 有冲突风险，UUID 各节点独立生成，保证全局唯一。
 * 生产环境也可用雪花算法（Snowflake）获得有序 ID。</p>
 *
 * @author chaos
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final JdbcTemplate jdbcTemplate;

    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建订单。
     *
     * @param userId    下单用户
     * @param productId 商品 ID
     * @param amount    订单金额
     * @return 订单编号
     */
    @Transactional(rollbackFor = Exception.class)
    public String create(String userId, String productId, double amount) {
        String orderNo = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update(
                "INSERT INTO order_tbl (user_id, product_id, amount, status, order_no) VALUES (?, ?, ?, 1, ?)",
                userId, productId, amount, orderNo);
        log.info("[AT] 订单创建成功: orderNo={}, userId={}, productId={}, amount={}",
                orderNo, userId, productId, amount);
        return orderNo;
    }

    /**
     * 查询订单（只读）。
     */
    public Order getByOrderNo(String orderNo) {
        return jdbcTemplate.query(
                "SELECT id, user_id, product_id, amount, status, order_no FROM order_tbl WHERE order_no = ?",
                (rs, rowNum) -> new Order(
                        rs.getLong("id"),
                        rs.getString("user_id"),
                        rs.getString("product_id"),
                        rs.getDouble("amount"),
                        rs.getInt("status"),
                        rs.getString("order_no")
                ),
                orderNo)
                .stream().findFirst().orElse(null);
    }

    /**
     * 获取订单数（测试断言用）。
     */
    public int count() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_tbl", Integer.class);
        return c != null ? c : 0;
    }
}
