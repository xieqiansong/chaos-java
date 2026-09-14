package lan.chaos.microservice.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 订单实体，对应 MySQL 的 {@code t_order}（Seata 全局事务的「订单侧分支资源」）。
 *
 * <p>{@code status} 取值：{@code CREATED}（正常创建）/ {@code DEGRADED}（P2 路径：user 服务降级，用户名未知）。
 * {@code id} 由数据库自增（AT 模式写入后回填）。</p>
 */
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private BigDecimal amount;
    private String status;

    public static Order sample(Long userId, BigDecimal amount) {
        Order o = new Order();
        o.setUserId(userId);
        o.setAmount(amount);
        return o;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
