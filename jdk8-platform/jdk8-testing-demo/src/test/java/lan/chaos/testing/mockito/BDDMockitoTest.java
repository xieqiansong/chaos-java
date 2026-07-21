package lan.chaos.testing.mockito;

import lan.chaos.testing.common.model.User;
import lan.chaos.testing.common.repository.UserRepository;
import lan.chaos.testing.common.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.*;

/**
 * 能力四：BDD（行为驱动开发）风格 —— given / when / then 可读性。
 *
 * <p>将 stub 放在每个测试方法内部，避免 Mockito 严格模式下的 UnnecessaryStubbingException。
 */
@ExtendWith(MockitoExtension.class)
class BDDMockitoTest {

    @Mock
    private UserRepository mockRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void givenUserExists_whenGetUserName_thenReturnName() {
        given(mockRepository.findById(1L))
                .willReturn(Optional.of(User.sampleUser()));

        String name = userService.getUserName(1L);
        assertEquals("张三", name);
        then(mockRepository).should().findById(1L);
    }

    @Test
    void givenUserNotExists_whenGetUserName_thenReturnUnknown() {
        given(mockRepository.findById(999L))
                .willReturn(Optional.empty());

        String name = userService.getUserName(999L);
        assertEquals("Unknown", name);
        then(mockRepository).should().findById(999L);
    }

    @Test
    void givenSave_whenCreateUser_thenUserSaved() {
        given(mockRepository.save(any(User.class)))
                .willReturn(User.sampleUser(100L, "新用户"));

        User created = userService.createUser("新用户", "new@test.com");
        assertEquals("新用户", created.getName());
        then(mockRepository).should().save(any(User.class));
    }

    @Test
    void givenNoCalls_whenOnlyQueried_thenSaveNeverCalled() {
        given(mockRepository.findById(1L))
                .willReturn(Optional.of(User.sampleUser()));

        userService.getUserName(1L);
        userService.countUsers();

        then(mockRepository).should(never()).save(any());
    }
}
