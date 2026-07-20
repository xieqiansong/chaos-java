package lan.chaos.mapstruct.collection;

import lan.chaos.mapstruct.basic.BasicMapper;
import lan.chaos.mapstruct.basic.UserDto;
import lan.chaos.mapstruct.common.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Set;

/**
 * 集合映射（对应官方示例 {@code mapstruct-iterable-to-non-iterable} 等）。
 * <p>
 * 只要存在「单元素映射方法」，MapStruct 就会自动复用并批量处理集合。
 * 这里通过 {@code uses = BasicMapper.class} 复用 {@link BasicMapper#toDto}，
 * 因此本接口只需声明集合方法。
 */
@Mapper(uses = BasicMapper.class)
public interface CollectionMapper {

    CollectionMapper INSTANCE = Mappers.getMapper(CollectionMapper.class);

    List<UserDto> toDtoList(List<User> users);

    Set<UserDto> toDtoSet(Set<User> users);
}
