package lan.chaos.microservice.common.security.model;

import java.util.HashSet;
import java.util.Set;

/**
 * 当前登录用户（从 JWT 解析而来，放进 ThreadLocal 供 Controller/Service 取用）。
 *
 * <p>WHY：微服务里「当前是谁、有什么权限」不应每次都查库，JWT 已自包含这些声明，
 * 解析一次放进上下文即可。敏感信息（密码等）绝不进这里。</p>
 */
public class LoginUser {

    /** 用户 ID。 */
    private Long userId;

    /** 用户名（subject）。 */
    private String username;

    /** 权限标识集合，如 {@code user:read}、{@code order:write}（与 @RequiresPermission 的值对应）。 */
    private Set<String> permissions = new HashSet<>();

    public LoginUser() {
    }

    public LoginUser(Long userId, String username, Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.permissions = permissions == null ? new HashSet<>() : permissions;
    }

    /** 是否拥有某权限（精确匹配，不做通配展开，保持简单可预测）。 */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
