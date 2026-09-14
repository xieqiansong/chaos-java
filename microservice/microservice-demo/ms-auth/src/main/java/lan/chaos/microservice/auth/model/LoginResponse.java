package lan.chaos.microservice.auth.model;

import java.util.List;

/**
 * 登录/刷新成功响应：双令牌 + 当前用户身份（下游可直接信任）。
 */
public class LoginResponse {

    /** 访问令牌（短命，无状态，网关/下游据此鉴权）。 */
    private String accessToken;

    /** 刷新令牌（长命，存服务端可吊销，用于换发新 access）。 */
    private String refreshToken;

    /** token 类型，固定 "Bearer"。 */
    private String tokenType = "Bearer";

    /** access 剩余有效期（秒），前端据此提前刷新。 */
    private long expiresIn;

    private Long userId;

    private String username;

    private List<String> permissions;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, long expiresIn,
                         Long userId, String username, List<String> permissions) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.username = username;
        this.permissions = permissions;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
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

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
