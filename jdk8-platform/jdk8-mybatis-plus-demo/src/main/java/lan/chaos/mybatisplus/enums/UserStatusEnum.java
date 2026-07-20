package lan.chaos.mybatisplus.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户状态枚举。
 * 高阶用法：用 {@link EnumValue} 标记「存库值」，MP 会自动在 枚举 <-> 数据库 int 之间转换，
 * 避免手写 code/desc 的互相映射，且比裸 int 更具可读性。
 */
@Getter
public enum UserStatusEnum {
    NORMAL(0, "正常"),
    FROZEN(1, "冻结"),
    DELETED(2, "注销");

    @EnumValue
    private final int code;
    private final String desc;

    UserStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
