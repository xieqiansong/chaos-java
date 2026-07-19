package lan.chaos.mapstruct.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 共享领域实体，作为 basic / collection / custom 各分类映射的源对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User implements Serializable {
    private String id;
    private String username;
    // 实际存储的是 BCrypt 加密后的哈希值
    private String password;
    private String email;
    private String phone;
    private String realName;
    private Gender gender;
    private Role role;
    private LocalDate birthday;
    private LocalDateTime createdAt;
    private Boolean enabled;
    private String level;
    private Address address;
}
