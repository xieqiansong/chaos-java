package lan.chaos.localcache.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 演示实体：本地缓存里存的对象。自带 sample() 工厂，调用方无需自己准备数据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private int age;

    /** 样例工厂：默认造一个可读的 User，便于直接跑场景。 */
    public static User sample(Long id) {
        return new User(id, "user-" + id, 20 + id.intValue());
    }
}
