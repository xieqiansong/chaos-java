package lan.chaos.mybatisplus.common.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lan.chaos.mybatisplus.common.enums.UserStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，集中演示多个高阶特性：
 *  - 枚举字段 {@link UserStatusEnum}（@EnumValue 自动存 code）
 *  - 乐观锁 {@link Version}
 *  - 逻辑删除 {@link TableLogic}
 *  - 自动填充 createTime / updateTime / operator（见 AuditMetaObjectHandler）
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer age;
    private String email;
    private UserStatusEnum status;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String operator;
}
