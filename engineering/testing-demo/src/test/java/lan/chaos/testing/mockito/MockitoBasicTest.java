package lan.chaos.testing.mockito;

import lan.chaos.testing.common.model.User;
import lan.chaos.testing.common.repository.UserRepository;
import lan.chaos.testing.common.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * 能力一：Mockito 核心三注解 —— @Mock / @Spy / @InjectMocks。
 *
 * <p>WHY：这是 Mockito 最常用的三个注解，理解它们的区别是写出正确测试的前提：
 * <ul>
 *   <li>@Mock：完全模拟，所有方法返回默认值（null/0/false），需手动 stub（when...thenReturn）</li>
 *   <li>@Spy：部分模拟，保留真实对象，可以对特定方法做 stub，其余保持真实行为</li>
 *   <li>@InjectMocks：自动将 @Mock/@Spy 注入到目标对象中（构造器注入/setter/字段注入）</li>
 * </ul>
 *
 * <p>关键对比：
 * <pre>
 *   @Mock  UserRepository repo;     // 完全虚拟，findById() 默认返回 null
 *   @Spy   UserRepository repo;     // 需要真实实例，findById() 走真实逻辑
 *   @Spy   UserService spyService;  // 需要真实实例，复用大部分真实逻辑
 * </pre>
 *
 * <p>生产坑：
 * <ul>
 *   <li>@Spy 注解在接口上无效（必须 concrete class），接口只能用 @Mock</li>
 *   <li>@Spy 对象 stub 时要用 doReturn().when() 而不是 when().thenReturn()，
 *       因为后者会触发真实方法调用</li>
 *   <li>@InjectMocks 注入失败不会报错，只是字段为 null（静默失败）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MockitoBasicTest {

    /** 完全 mock：所有返回值是默认值 */
    @Mock
    private UserRepository mockRepository;

    /** 真实 service，自动注入 mockRepository */
    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.sampleUser();
    }

    // ========== @Mock 演示 ==========

    /**
     * @Mock：默认行为 → findById 返回 Optional.empty() → getUserName 返回 "Unknown"。
     */
    @Test
    void mock_defaultBehavior_returnsDefaultValues() {
        // 未 stub → mock 方法返回 Optional.empty()（默认值）
        String name = userService.getUserName(1L);
        assertEquals("Unknown", name, "未 stub 时返回默认值");
    }

    /**
     * @Mock + stub：预设返回值 → getUserName 返回预设的名称。
     */
    @Test
    void mock_withStub_returnsStubbedValue() {
        // 输入 → 输出：stub findById(1L) 返回 sampleUser
        when(mockRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        String name = userService.getUserName(1L);
        assertEquals(sampleUser.getName(), name, "stub 后应返回预设名称");
    }

    /**
     * @Mock + stub：预设不同参数返回不同值。
     * 演示了「输入 → 输出」可观察性：不同的输入产生不同的 stub 输出。
     */
    @Test
    void mock_withDifferentParams_returnsDifferentResults() {
        when(mockRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(mockRepository.findById(999L)).thenReturn(Optional.empty());

        assertEquals("张三", userService.getUserName(1L), "id=1 应返回预设");
        assertEquals("Unknown", userService.getUserName(999L), "id=999 应返回 Unknown");
    }

    // ========== @Spy 演示 ==========

    /**
     * @Spy：对真实方法进行 stub，保留其他方法真实行为。
     * 注意：spy 必须用 doReturn().when()，否则 when() 内的真实方法会被调用！
     */
    @Test
    void spy_partialMock_overridesSpecificMethod() {
        // 创建真实对象再 spy
        UserService spyService = spy(new UserService(mockRepository));
        when(mockRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        // spy 部分 mock：只 stub formatUser 方法
        doReturn("[REDACTED]").when(spyService).formatUser(any(User.class));

        // getUserSummary 内部调用了 formatUser → 走 stub
        String summary = spyService.getUserSummary(1L);
        assertEquals("[REDACTED]", summary);

        // 验证 spy 的 formatUser 确实被调用了
        verify(spyService).formatUser(any(User.class));
    }
}
