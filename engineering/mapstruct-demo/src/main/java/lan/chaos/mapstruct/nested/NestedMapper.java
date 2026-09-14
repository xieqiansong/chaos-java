package lan.chaos.mapstruct.nested;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 嵌套对象映射（对应官方示例 {@code mapstruct-nested-bean-mappings}）。
 * <p>
 * 当源与目标字段同名且类型可映射时，MapStruct 会递归处理嵌套 Bean，无需额外配置。
 * 这是一个自包含示例：{@link Person} 中的 {@link Address} 会自动映射为 {@link PersonDto} 中的 {@link AddressDto}。
 */
@Mapper
public interface NestedMapper {

    NestedMapper INSTANCE = Mappers.getMapper(NestedMapper.class);

    PersonDto toDto(Person person);
}
