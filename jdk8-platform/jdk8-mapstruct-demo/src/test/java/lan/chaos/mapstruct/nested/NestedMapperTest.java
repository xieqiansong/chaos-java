package lan.chaos.mapstruct.nested;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NestedMapperTest {

    @Test
    void toDto_mapsNestedBean() {
        Person person = Person.builder()
                .name("张三")
                .address(Address.builder().street("科技园路 1 号").city("深圳").build())
                .build();

        PersonDto dto = NestedMapper.INSTANCE.toDto(person);

        assertEquals("张三", dto.getName());
        // 嵌套 Address 自动递归映射
        assertNotNull(dto.getAddress());
        assertEquals("深圳", dto.getAddress().getCity());
        assertEquals("科技园路 1 号", dto.getAddress().getStreet());
    }

    @Test
    void toDto_nullNestedIsSafe() {
        Person person = Person.builder().name("李四").build();

        PersonDto dto = NestedMapper.INSTANCE.toDto(person);

        assertEquals("李四", dto.getName());
        // 源嵌套对象为 null 时，目标嵌套对象也为 null（不会 NPE）
        assertEquals(null, dto.getAddress());
    }
}
