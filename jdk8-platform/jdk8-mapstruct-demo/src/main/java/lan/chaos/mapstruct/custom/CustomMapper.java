package lan.chaos.mapstruct.custom;

import lan.chaos.mapstruct.basic.AddressDto;
import lan.chaos.mapstruct.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 自定义映射（对应官方示例中的 qualifier / expression / date-conversion）：
 * <ul>
 *     <li>{@link #toSummary} 字段名不一致 / 忽略 / 常量 / 默认值 / 表达式</li>
 *     <li>{@link #toCard} 嵌套 / 日期格式化 / qualifiedByName / 表达式</li>
 * </ul>
 */
@Mapper(uses = MappingUtil.class)
public interface CustomMapper {

    CustomMapper INSTANCE = Mappers.getMapper(CustomMapper.class);

    /**
     * 摘要映射，演示字段名不一致、忽略、常量、默认值、表达式。
     */
    @Mapping(source = "id", target = "userId")
    @Mapping(source = "username", target = "account")
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "type", constant = "SUMMARY")
    @Mapping(source = "level", target = "grade", defaultValue = "NORMAL")
    @Mapping(target = "displayName", expression = "java(user.getRealName() + \"(\" + user.getUsername() + \")\")")
    UserSummaryDto toSummary(User user);

    /**
     * 卡片映射，演示嵌套、日期格式化与自定义类型转换。
     */
    @Mapping(source = "gender", target = "genderDesc", qualifiedByName = "genderToDesc")
    @Mapping(source = "role", target = "roleDesc", qualifiedByName = "roleToDesc")
    @Mapping(source = "birthday", target = "birthdayStr", dateFormat = "yyyy-MM-dd")
    @Mapping(source = "createdAt", target = "createdAtStr", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "addressDetail",
            expression = "java(user.getAddress() == null ? \"\" : " +
                    "user.getAddress().getProvince() + user.getAddress().getCity() + user.getAddress().getDetail())")
    UserCardDto toCard(User user);
}
