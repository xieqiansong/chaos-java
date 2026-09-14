package lan.chaos.serialization.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 序列化 demo 的样例数据模型。
 *
 * <p>同时实现 {@link Serializable}（JDK 原生序列化需要）、提供无参构造（Jackson/Kryo 反射需要）、
 * 用 Lombok {@code @Data} 生成 getter/setter/equals，方便三种序列化方式做「往返后字段是否一致」的断言。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    private Date birthday;
    private List<String> roles;

    /** 样例工厂：调用方无需自己准备输入。 */
    public static User sampleUser() {
        User u = new User();
        u.id = 1L;
        u.name = "Alice";
        u.email = "alice@example.com";
        // 1990-01-01（Date 已过时但序列化 demo 需覆盖日期类型）
        u.birthday = new Date(90, 0, 1);
        u.roles = Arrays.asList("USER", "ADMIN");
        return u;
    }
}
