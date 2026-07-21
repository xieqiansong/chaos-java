package lan.chaos.testing.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 样例数据模型：用户实体。
 *
 * <p>用于演示 Mockito mock/spy/stub 和 Spring Boot 切片测试中的参数构造。
 * 提供 {@link #sampleUser()} 工厂方法，调用方无需自己准备输入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createTime;

    /** 样例用户 */
    public static User sampleUser() {
        return User.builder()
                .id(1L)
                .name("张三")
                .email("zhangsan@example.com")
                .createTime(LocalDateTime.now())
                .build();
    }

    /** 样例用户列表 */
    public static User sampleUser(long id, String name) {
        return User.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase() + "@example.com")
                .createTime(LocalDateTime.now())
                .build();
    }
}
