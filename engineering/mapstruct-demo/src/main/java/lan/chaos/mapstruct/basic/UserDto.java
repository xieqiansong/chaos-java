package lan.chaos.mapstruct.basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 与 {@link lan.chaos.mapstruct.model.User} 字段一一对应的完整 DTO，用于演示基础映射与反向映射。
 * 包含嵌套的 {@link AddressDto}，因此本类同时覆盖「嵌套对象映射」。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto implements Serializable {
    private String id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String realName;
    private String gender;
    private String role;
    private LocalDate birthday;
    private LocalDateTime createdAt;
    private Boolean enabled;
    private String level;
    private AddressDto address;
}
