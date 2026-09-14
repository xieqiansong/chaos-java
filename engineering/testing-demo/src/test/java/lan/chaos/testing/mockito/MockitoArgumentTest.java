package lan.chaos.testing.mockito;

import lan.chaos.testing.common.model.User;
import lan.chaos.testing.common.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 能力二：Mockito 参数匹配（Argument Matchers）—— 灵活的 stub 条件。
 *
 * <p>WHY：when(mock.method(specificValue)) 只能匹配精确参数。用参数匹配器可以
 * 匹配任意值、类型、模式、自定义条件，写出更灵活和健壮的测试。
 *
 * <p>关键匹配器：
 * <ul>
 *   <li>any() / any(String.class) — 匹配任意值</li>
 *   <li>eq(value) — 精确匹配（与 any 混合使用时必须）</li>
 *   <li>argThat(condition) — 自定义条件匹配</li>
 *   <li>nullable(Class) — 匹配 null 或该类型</li>
 * </ul>
 *
 * <p>生产坑：
 * <ul>
 *   <li>如果混合使用 any() 和具体值，所有参数都必须用匹配器：
 *       错误: when(mock.method("exact", anyString()))  → 编译通过但运行时抛异常</li>
 *   <li>原始类型用 anyInt()/anyLong()/anyBoolean()，不要用 any(Integer.class)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MockitoArgumentTest {

    @Mock
    private UserRepository mockRepository;

    @Test
    void argThat_customMatcher_saveOnlyVipUsers() {
        User vipUser = User.sampleUser(1L, "VIP_张三");
        User normalUser = User.sampleUser(2L, "普通用户");

        when(mockRepository.save(argThat(u -> u.getName() != null && u.getName().startsWith("VIP_"))))
                .thenReturn(vipUser);

        User saved = mockRepository.save(vipUser);
        assertEquals(vipUser, saved, "VIP 用户应被保存");

        User notSaved = mockRepository.save(normalUser);
        assertNull(notSaved, "非 VIP 用户未匹配 stub");
    }

    @Test
    void any_withEq_mixedMatching() {
        when(mockRepository.findById(anyLong())).thenReturn(Optional.empty());

        Optional<User> result = mockRepository.findById(999L);
        assertFalse(result.isPresent(), "任何 ID 都应返回 empty");
    }

    @Test
    void anyMatchers_overview() {
        when(mockRepository.findById(any(Long.class))).thenReturn(Optional.empty());
        when(mockRepository.findAll()).thenReturn(java.util.Collections.emptyList());

        Optional<User> r1 = mockRepository.findById(0L);
        Optional<User> r2 = mockRepository.findById(Long.MAX_VALUE);
        Optional<User> r3 = mockRepository.findById(null);
        List<User> r4 = mockRepository.findAll();

        assertFalse(r1.isPresent(), "anyLong 匹配");
        assertFalse(r2.isPresent(), "anyLong 匹配");
        // any(Long.class) 也匹配 null，返回 stub 的 Optional.empty()
        assertFalse(r3.isPresent(), "any(Long.class) 也匹配 null");
        assertTrue(r4.isEmpty(), "findAll 返回空");
    }

    @Test
    void matches_regexPattern_specialStubForCompanyEmail() {
        when(mockRepository.save(argThat(u -> u != null && u.getEmail() != null
                && u.getEmail().endsWith("@company.com"))))
                .thenReturn(User.sampleUser(999L, "企业用户"));

        User companyUser = User.builder().name("员工A").email("a@company.com").build();
        User personalUser = User.builder().name("普通B").email("b@gmail.com").build();

        User r1 = mockRepository.save(companyUser);
        User r2 = mockRepository.save(personalUser);

        assertNotNull(r1, "企业邮箱应匹配 stub");
        assertNull(r2, "个人邮箱不匹配 stub");
    }
}
