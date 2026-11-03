package lan.chaos.dubbo.api.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RPC 传输模型。
 *
 * <p>WHY：Dubbo 默认用 Hessian2 序列化跨网络传输参数与返回值，被传输的对象必须实现
 * {@link Serializable}，否则 provider 序列化阶段直接抛异常。模型尽量「贫血、可序列化、字段稳定」，
 * 避免把 JPA 实体 / 业务对象直接透传（字段变更会破坏两端兼容性）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 id。 */
    private Long id;

    /** 用户名。 */
    private String name;

    /** 年龄。 */
    private Integer age;

    /**
     * 服务实例标识：哪个 provider 实例真正处理了本次请求。
     * 演示「负载均衡 / 注册发现」时用它观察请求被路由到了哪个实例。
     */
    private String instanceId;

    /** 自带样例数据工厂：调用方无需自己准备输入（AGENTS.md 强制要求）。 */
    public static User sample(Long id) {
        return User.builder()
                .id(id)
                .name("user-" + id)
                .age(20 + (int) (id % 30))
                .instanceId("default")
                .build();
    }

    /** 带实例标识的样例工厂（负载均衡演示用：区分 A / B 实例）。 */
    public static User sample(Long id, String instanceId) {
        User u = sample(id);
        u.setInstanceId(instanceId);
        return u;
    }
}
