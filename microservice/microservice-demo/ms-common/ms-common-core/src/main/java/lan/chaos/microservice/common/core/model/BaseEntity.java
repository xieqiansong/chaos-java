package lan.chaos.microservice.common.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计基类：所有带“创建/更新时间 + 逻辑删除”的表实体共用。
 *
 * <p>WHY：把 createdAt / updatedAt / deleted 抽到基类，避免每个实体重复声明；
 * 由 MyBatis-Plus 的 {@code MetaObjectHandler} 自动填充时间，由全局 logic-delete 配置自动处理 deleted，
 * 业务代码不感知。注意：{@code id} 不放在基类——各实体主键类型/注解不同，避免字段遮蔽问题。</p>
 *
 * <p>生产坑：逻辑删除字段用 {@code @JsonIgnore} 避免泄露给前端；本类不引入 MyBatis-Plus 注解，
 * 逻辑删除通过 application.yml 中 {@code mybatis-plus.global-config.db-config.logic-delete-field} 全局配置。</p>
 */
public abstract class BaseEntity implements Serializable {

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonIgnore
    private Integer deleted;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
