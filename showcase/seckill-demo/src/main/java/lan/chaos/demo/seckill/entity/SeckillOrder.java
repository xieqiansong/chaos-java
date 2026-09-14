package lan.chaos.demo.seckill.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体
 */
@Entity
@Table(name = "t_seckill_order", indexes = {
        @Index(name = "idx_order_token", columnList = "token", unique = true),
        @Index(name = "idx_order_user_product", columnList = "user_id, product_id")
})
public class SeckillOrder {

    @Id
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "token", length = 64, nullable = false, unique = true)
    private String token;

    @Column(name = "bucket_index", nullable = false)
    private Integer bucketIndex = 0;

    @Column(name = "status", length = 16, nullable = false)
    private String status = "PENDING";

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // ===== Getters & Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getBucketIndex() {
        return bucketIndex;
    }

    public void setBucketIndex(Integer bucketIndex) {
        this.bucketIndex = bucketIndex;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
