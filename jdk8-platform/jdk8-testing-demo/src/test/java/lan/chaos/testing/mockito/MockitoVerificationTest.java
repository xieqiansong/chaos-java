package lan.chaos.testing.mockito;

import lan.chaos.testing.common.model.User;
import lan.chaos.testing.common.repository.UserRepository;
import lan.chaos.testing.common.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * 能力三：Mockito 行为验证（Verification）—— 确认方法是否被调用、调用次数、顺序。
 *
 * <p>每个测试方法独立 stub，避免 Mockito 严格模式下的 UnnecessaryStubbingException。
 */
@ExtendWith(MockitoExtension.class)
class MockitoVerificationTest {

    @Mock
    private UserRepository mockRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void verify_basic_methodWasCalled() {
        when(mockRepository.save(any(User.class))).thenReturn(User.sampleUser());
        userService.createUser("test", "test@test.com");
        verify(mockRepository).save(any(User.class));
    }

    @Test
    void verify_withTimes_exactCallCount() {
        when(mockRepository.save(any(User.class))).thenReturn(User.sampleUser());
        userService.createUser("A", "a@test.com");
        userService.createUser("B", "b@test.com");
        userService.createUser("C", "c@test.com");
        verify(mockRepository, times(3)).save(any(User.class));
    }

    @Test
    void verify_never_methodNotCalled() {
        when(mockRepository.findById(1L)).thenReturn(Optional.of(User.sampleUser()));
        userService.getUserName(1L);
        verify(mockRepository, never()).save(any(User.class));
    }

    @Test
    void verify_atLeastAtMost_callCountRange() {
        when(mockRepository.save(any(User.class))).thenReturn(User.sampleUser());
        userService.createUser("A", "a@x.com");
        userService.createUser("B", "b@x.com");
        verify(mockRepository, atLeast(1)).save(any(User.class));
        verify(mockRepository, atMost(5)).save(any(User.class));
    }

    @Test
    void verify_inOrder_callSequence() {
        when(mockRepository.findById(1L)).thenReturn(Optional.of(User.sampleUser()));
        when(mockRepository.save(any(User.class))).thenReturn(User.sampleUser());

        userService.getUserName(1L);
        userService.createUser("new", "new@x.com");

        InOrder inOrder = inOrder(mockRepository);
        inOrder.verify(mockRepository).findById(1L);
        inOrder.verify(mockRepository).save(any());
    }

    @Test
    void verify_argThat_checkPassedArgument() {
        when(mockRepository.save(any(User.class))).thenReturn(User.sampleUser());
        userService.createUser("VIP_Test", "vip@vip.com");
        verify(mockRepository).save(argThat(user ->
                user.getName().equals("VIP_Test") && user.getEmail().equals("vip@vip.com")));
    }
}
