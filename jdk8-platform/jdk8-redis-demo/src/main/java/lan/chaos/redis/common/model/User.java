package lan.chaos.redis.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 演示实体：用于对象缓存场景。
 *
 * <p>通过 {@code RedisConfig} 中的 JSON 序列化器存入 Redis，value 为可读 JSON（含 {@code @class} 类型信息）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer age;
}
