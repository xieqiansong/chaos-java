package lan.chaos.microservice.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lan.chaos.microservice.common.core.model.BaseEntity;

/**
 * 用户标签实体（第二数据源 MySQL，表 t_user_tag）。
 *
 * <p>WHY：专门放在 MySQL 是为了演示“同一个应用内切换异构数据源”——service 层打
 * {@code @DS("mysql")} 即可路由到这里，而 {@link User} 走默认主库 PG。
 * 注意 user_id 字段在 MySQL 中用 {@code bigint unsigned}，跨库演示“字段类型语义差异”。</p>
 */
@TableName("t_user_tag")
public class UserTag extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String tag;

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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
