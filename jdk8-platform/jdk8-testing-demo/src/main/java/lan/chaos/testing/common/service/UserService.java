package lan.chaos.testing.common.service;

import lan.chaos.testing.common.model.User;
import lan.chaos.testing.common.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户服务 — 被测试的目标类。
 *
 * <p>设计为依赖 UserRepository（接口），测试时通过 @InjectMocks + @Mock 注入。
 * 覆盖三种常见测试场景：
 * <ol>
 *   <li>mock 外部依赖 → stub 返回值，验证业务逻辑</li>
 *   <li>spy 部分 mock → 保留真实方法，只 stub 特定调用</li>
 *   <li>verify 行为验证 → 确认依赖方法被正确调用（次数/参数）</li>
 * </ol>
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 根据 ID 获取用户名，不存在时返回 "Unknown"。
     * 典型 mock + stub 场景。
     */
    public String getUserName(Long id) {
        return userRepository.findById(id)
                .map(User::getName)
                .orElse("Unknown");
    }

    /**
     * 创建用户 — 需要调用 repository.save()。
     * 典型 verify 场景：验证 save 被调用且参数正确。
     */
    public User createUser(String name, String email) {
        User user = User.builder()
                .name(name)
                .email(email)
                .build();
        return userRepository.save(user);
    }

    /**
     * 获取用户摘要信息 — 拼接 ID + 名称。
     * 演示 spy 场景：可以 spy UserService 本身。
     */
    public String getUserSummary(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return formatUser(user.get());
        }
        return "User Not Found: " + id;
    }

    /**
     * 公开的格式化方法 — spy 时可以被部分 mock。
     */
    public String formatUser(User user) {
        return "[" + user.getId() + "] " + user.getName();
    }

    /**
     * 统计用户总数。
     */
    public long countUsers() {
        return userRepository.count();
    }
}
