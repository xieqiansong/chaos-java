package lan.chaos.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * 拷贝
     */
    User clone(User user);


    /**
     * 基本映射
     */
    UserDTO toDTO(User user);

    /**
     * 反向映射
     */
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "lastLoginTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User fromDTO(UserDTO userDTO);

    /**
     * 忽略某些字段
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "lastLoginTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntityIgnoreId(UserDTO userDTO);

    /**
     * 自定义映射规则
     */

    @Mapping(target = "username", expression = "java(user.getUsername())")
    UserDTO toCustomDTO(User user);
}
