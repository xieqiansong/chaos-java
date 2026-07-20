package lan.chaos.mapstruct.basic;

import lan.chaos.mapstruct.common.model.Address;
import lan.chaos.mapstruct.common.model.Gender;
import lan.chaos.mapstruct.common.model.Role;
import lan.chaos.mapstruct.common.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class BasicMapperTest {

    private User sampleUser() {
        return User.builder()
                .id("1")
                .username("root")
                .password("root")
                .email("root@confusion.lan")
                .phone("12388888888")
                .realName("管理员")
                .gender(Gender.MALE)
                .role(Role.ADMIN)
                .birthday(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.of(2026, 7, 19, 10, 0, 0))
                .enabled(true)
                .level("VIP")
                .address(Address.builder().province("广东").city("深圳").detail("科技园").build())
                .build();
    }

    @Test
    void clone_isNewObjectWithSameFields() {
        User user = sampleUser();
        User clone = BasicMapper.INSTANCE.clone(user);

        assertNotSame(user, clone);
        assertEquals(user.getId(), clone.getId());
        assertEquals(user.getUsername(), clone.getUsername());
        assertEquals(user.getAddress(), clone.getAddress());
    }

    @Test
    void toDto_mapsAllFieldsIncludingNested() {
        User user = sampleUser();
        UserDto dto = BasicMapper.INSTANCE.toDto(user);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(user.getPassword(), dto.getPassword());
        assertEquals("MALE", dto.getGender());
        assertEquals("ADMIN", dto.getRole());
        assertEquals(user.getCreatedAt(), dto.getCreatedAt());
        // 嵌套映射
        assertEquals(user.getAddress().getCity(), dto.getAddress().getCity());
    }

    @Test
    void fromDto_isInverseOfToDto() {
        User user = sampleUser();
        UserDto dto = BasicMapper.INSTANCE.toDto(user);
        User back = BasicMapper.INSTANCE.fromDto(dto);

        assertEquals(user.getId(), back.getId());
        assertEquals(user.getUsername(), back.getUsername());
        assertEquals(user.getGender(), back.getGender());
        assertEquals(user.getRole(), back.getRole());
        assertEquals(user.getAddress().getCity(), back.getAddress().getCity());
    }

    @Test
    void clone_nullInput() {
        assertNull(BasicMapper.INSTANCE.clone(null));
    }
}
