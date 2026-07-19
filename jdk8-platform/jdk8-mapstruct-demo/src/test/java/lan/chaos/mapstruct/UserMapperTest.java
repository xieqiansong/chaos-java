package lan.chaos.mapstruct;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMapperTest {

    private User sampleUser() {
        return User.builder()
                .id("1")
                .username("root")
                .password("root")
                .email("root@confusion.lan")
                .phone("12388888888")
                .enabled(true)
                .locked(false)
                .lastLoginTime(LocalDateTime.of(2026, 7, 19, 10, 0))
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .build();
    }

    @Test
    void testClone() {
        User user = sampleUser();

        User clone = UserMapper.INSTANCE.clone(user);

        assertNotNull(clone);
        // 拷贝后字段值应完全一致
        assertEquals(user.getId(), clone.getId());
        assertEquals(user.getUsername(), clone.getUsername());
        assertEquals(user.getPassword(), clone.getPassword());
        assertEquals(user.getEmail(), clone.getEmail());
        assertEquals(user.getPhone(), clone.getPhone());
        assertEquals(user.getEnabled(), clone.getEnabled());
        assertEquals(user.getLocked(), clone.getLocked());
        assertEquals(user.getLastLoginTime(), clone.getLastLoginTime());
        assertEquals(user.getCreatedAt(), clone.getCreatedAt());
        assertEquals(user.getUpdatedAt(), clone.getUpdatedAt());
        // 应是新对象，深拷贝语义（引用不同）
        assertNotSame(user, clone);
    }

    @Test
    void toDTO() {
        User user = sampleUser();

        UserDTO dto = UserMapper.INSTANCE.toDTO(user);

        assertNotNull(dto);
        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(user.getPassword(), dto.getPassword());
        assertEquals(user.getEmail(), dto.getEmail());
        assertEquals(user.getPhone(), dto.getPhone());
    }

    @Test
    void fromDTO() {
        User user = sampleUser();
        UserDTO dto = UserMapper.INSTANCE.toDTO(user);

        User newUser = UserMapper.INSTANCE.fromDTO(dto);

        assertNotNull(newUser);
        // 基础字段映射
        assertEquals(dto.getId(), newUser.getId());
        assertEquals(dto.getUsername(), newUser.getUsername());
        assertEquals(dto.getPassword(), newUser.getPassword());
        assertEquals(dto.getEmail(), newUser.getEmail());
        assertEquals(dto.getPhone(), newUser.getPhone());
        // 被忽略的字段使用默认值 / null
        assertTrue(newUser.getEnabled());
        assertEquals(Boolean.FALSE, newUser.getLocked());
        assertNull(newUser.getLastLoginTime());
        assertNull(newUser.getCreatedAt());
        assertNull(newUser.getUpdatedAt());
    }

    @Test
    void toEntityIgnoreId() {
        User user = sampleUser();
        UserDTO dto = UserMapper.INSTANCE.toDTO(user);

        User newUser = UserMapper.INSTANCE.toEntityIgnoreId(dto);

        assertNotNull(newUser);
        // id 被忽略，应为 null
        assertNull(newUser.getId());
        // 其余基础字段映射
        assertEquals(dto.getUsername(), newUser.getUsername());
        assertEquals(dto.getPassword(), newUser.getPassword());
        assertEquals(dto.getEmail(), newUser.getEmail());
        assertEquals(dto.getPhone(), newUser.getPhone());
        // 被忽略的字段使用默认值 / null
        assertTrue(newUser.getEnabled());
        assertEquals(Boolean.FALSE, newUser.getLocked());
        assertNull(newUser.getLastLoginTime());
        assertNull(newUser.getCreatedAt());
        assertNull(newUser.getUpdatedAt());
    }

    @Test
    void toCustomDTO() {
        User user = sampleUser();

        UserDTO customDTO = UserMapper.INSTANCE.toCustomDTO(user);

        assertNotNull(customDTO);
        // 通过自定义 expression 映射 username
        assertEquals(user.getUsername(), customDTO.getUsername());
        // 其余字段按名称自动映射
        assertEquals(user.getId(), customDTO.getId());
        assertEquals(user.getPassword(), customDTO.getPassword());
        assertEquals(user.getEmail(), customDTO.getEmail());
        assertEquals(user.getPhone(), customDTO.getPhone());
    }

    @Test
    void toDTO_nullInput() {
        assertNull(UserMapper.INSTANCE.toDTO(null));
    }

    @Test
    void clone_nullInput() {
        assertNull(UserMapper.INSTANCE.clone(null));
    }
}
