package lan.chaos.mapstruct.basic;

import lan.chaos.mapstruct.model.User;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 基础映射（对应官方示例 {@code mapstruct-clone} / 字段映射）：
 * <ul>
 *     <li>{@link #clone} 同类型深拷贝</li>
 *     <li>{@link #toDto} 实体 -> DTO（含嵌套 Address 映射）</li>
 *     <li>{@link #fromDto} 反向映射，复用 toDto 配置并自动反转</li>
 * </ul>
 */
@Mapper
public interface BasicMapper {

    BasicMapper INSTANCE = Mappers.getMapper(BasicMapper.class);

    /**
     * 同类型深拷贝。
     */
    User clone(User user);

    /**
     * 基础映射：实体 -> 完整 DTO（字段同名自动映射，枚举按 name 映射为字符串，嵌套 Address 自动递归）。
     */
    UserDto toDto(User user);

    /**
     * 反向映射：复用 {@link #toDto} 的配置并自动反转 source/target。
     */
    @InheritInverseConfiguration(name = "toDto")
    User fromDto(UserDto userDto);
}
