package lan.chaos.demo.seckill.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Entity
@Table(name = "t_product")
public class Product {

    @Id
    private Long id;

    @Column(name = "product_name", length = 128, nullable = false)
    private String productName;

    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;

    @Column(name = "bucket_count", nullable = false)
    private Integer bucketCount = 10;

    @Column(name = "bucket_size", nullable = false)
    private Integer bucketSize;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status", length = 16, nullable = false)
    private String status = "DRAFT";

    @Version
    @Column(name = "version")
    private Integer version = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (bucketCount == null || bucketCount <= 0) {
            bucketCount = 10;
        }
        if (totalStock != null && totalStock > 0) {
            bucketSize = totalStock / bucketCount;
        }
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(Integer bucketCount) {
        this.bucketCount = bucketCount;
    }

    public Integer getBucketSize() {
        return bucketSize;
    }

    public void setBucketSize(Integer bucketSize) {
        this.bucketSize = bucketSize;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== 业务方法 =====

    /**
     * 判断商品是否处于秒杀活动时间内
     */
    public boolean isInSeckillPeriod() {
        LocalDateTime now = LocalDateTime.now();
        return "ACTIVE".equals(status)
                && startTime != null && !now.isBefore(startTime)
                && endTime != null && !now.isAfter(endTime);
    }

    /**
     * 判断是否已售罄
     */
    public boolean isSoldOut() {
        return "SOLD_OUT".equals(status);
    }
}
