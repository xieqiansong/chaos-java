package lan.chaos.microservice.order.model;

/**
 * 从 user 服务透传过来的用户视图（Feign 反序列化目标）。
 *
 * <p>字段刻意对齐 {@code lan.chaos.microservice.user.entity.User}，避免 Jackson 反序列化时丢字段。
 * 演示跨服务 DTO：order 不依赖 user 的持久层实体，只用它暴露的“视图”。</p>
 */
public class UserDTO {

    private Long id;
    private String username;
    private String nickname;
    private Integer age;
    private String phone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
