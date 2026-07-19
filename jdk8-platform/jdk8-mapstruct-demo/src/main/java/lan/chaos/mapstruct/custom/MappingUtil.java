package lan.chaos.mapstruct.custom;

import lan.chaos.mapstruct.model.Gender;
import lan.chaos.mapstruct.model.Role;
import org.mapstruct.Named;

/**
 * 自定义类型转换工具（对应官方示例 {@code mapstruct-rounding} 中的 qualifier 用法）。
 * <p>
 * 通过 {@link Named} 暴露方法，供 Mapper 的 {@code qualifiedByName} 引用。
 * 使用方式：在 Mapper 上声明 {@code @Mapper(uses = MappingUtil.class)}，
 * 再在 {@code @Mapping} 中通过 {@code qualifiedByName = "xxx"} 引用对应方法。
 */
public class MappingUtil {

    @Named("genderToDesc")
    public String genderToDesc(Gender gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case MALE:
                return "男";
            case FEMALE:
                return "女";
            default:
                return "未知";
        }
    }

    @Named("roleToDesc")
    public String roleToDesc(Role role) {
        if (role == null) {
            return "普通用户";
        }
        switch (role) {
            case ADMIN:
                return "管理员";
            case USER:
                return "普通用户";
            default:
                return "普通用户";
        }
    }
}
