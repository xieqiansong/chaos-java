package lan.chaos.microservice.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户余额实体，对应主库 PG 的 {@code t_account}。
 *
 * <p>刻意不继承 {@link lan.chaos.microservice.common.core.model.BaseEntity}：
 * AT 模式要求每张参与表都有独立的 undo_log，而 BaseEntity 自带的 {@code deleted} 逻辑删除列在
 * t_account 中不存在，继承会导致 SQL 误带 deleted 列报错。所以这里手写干净字段。</p>
 */
@TableName("t_account")
public class Account {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
