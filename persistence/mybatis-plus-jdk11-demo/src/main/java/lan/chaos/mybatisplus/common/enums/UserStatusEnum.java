package lan.chaos.mybatisplus.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;

/**
 * 用户状态枚举，演示 MyBatis-Plus 的枚举字段映射。
 *
 * <p>痛点：数据库里存的是整数/编码（1/2/3），Java 里想用语义化枚举。
 * 用 {@link EnumValue} 标记「存库值」字段，MyBatis-Plus 在读写时自动在
 * 枚举 ↔ code 之间转换，业务代码全程只接触枚举，不碰魔法数字。
 *
 * <p>生产坑：枚举新增取值要兼容历史数据；若用 {@code IEnum} 自定义 getValue，
 * 存库值以 getValue 为准；枚举顺序别随便调整，避免反序列化错位。
 */
public enum UserStatusEnum implements IEnum<Integer> {

    NORMAL(1, "正常"),
    FROZEN(2, "冻结"),
    DISABLED(3, "禁用");

    @EnumValue
    private final Integer code;
    private final String desc;

    UserStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}
