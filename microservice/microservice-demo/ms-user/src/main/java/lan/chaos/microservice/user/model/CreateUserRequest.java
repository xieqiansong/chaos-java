package lan.chaos.microservice.user.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建用户请求体。字段用 javax.validation 注解，由 GlobalExceptionHandler 统一收口校验失败。
 */
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于 0")
    private Integer age;

    @NotBlank(message = "手机号不能为空")
    private String phone;

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
