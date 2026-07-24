package lan.chaos.microservice.order.model;

import java.math.BigDecimal;

/**
 * 订单（P2 仅内存演示，不落库；落库与 Seata 在 P3 落地）。
 *
 * <p>{@code status} 取值：{@code CREATED}（正常、已填充用户名）/ {@code DEGRADED}（user 服务降级，用户名未知）</p>
 */
public class Order {

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
