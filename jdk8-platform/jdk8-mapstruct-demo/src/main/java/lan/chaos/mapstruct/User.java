package lan.chaos.mapstruct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User implements Serializable {
    private String id;
    private String username;
    // 实际存储的是BCrypt加密后的哈希值
    private String password;
    private String email;
    private String phone;
    // 账户是否启用
    @Builder.Default
    private Boolean enabled = true;
    // 账户是否锁定
    @Builder.Default
    private Boolean locked = false;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
