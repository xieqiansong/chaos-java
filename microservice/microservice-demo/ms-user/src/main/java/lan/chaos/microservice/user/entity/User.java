package lan.chaos.microservice.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lan.chaos.microservice.common.core.model.BaseEntity;

/**
 * 用户实体（主数据源 PostgreSQL，表 t_user）。
 *
 * <p>id 不放在 BaseEntity，这里用 @TableId(AUTO) 交给数据库自增（PG 用 identity / serial）。
 * 审计字段 created_at / updated_at / deleted 来自 {@link BaseEntity}，由 MP 自动填充与逻辑删除。</p>
 */
@TableName("t_user")
public class User extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private Integer age;
    private String phone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
