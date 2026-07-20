package lan.chaos.mapstruct.collection;

import lan.chaos.mapstruct.basic.UserDto;
import lan.chaos.mapstruct.common.model.Gender;
import lan.chaos.mapstruct.common.model.Role;
import lan.chaos.mapstruct.common.model.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CollectionMapperTest {

    private User user(String id) {
        return User.builder()
                .id(id)
                .username("u" + id)
                .gender(Gender.MALE)
                .role(Role.USER)
                .build();
    }

    @Test
    void toDtoList_mapsEveryElement() {
        List<User> users = Arrays.asList(user("1"), user("2"));

        List<UserDto> dtos = CollectionMapper.INSTANCE.toDtoList(users);

        assertEquals(2, dtos.size());
        assertEquals("1", dtos.get(0).getId());
        assertEquals("u2", dtos.get(1).getUsername());
    }

    @Test
    void toDtoSet_mapsEveryElement() {
        Set<User> users = new HashSet<>(Arrays.asList(user("1"), user("2")));

        Set<UserDto> dtos = CollectionMapper.INSTANCE.toDtoSet(users);

        assertEquals(2, dtos.size());
        dtos.forEach(d -> assertNotNull(d.getId()));
    }

    @Test
    void toDtoList_nullInput() {
        assertNull(CollectionMapper.INSTANCE.toDtoList(null));
    }
}
