package lan.chaos.oauth2.auth.token;

import lan.chaos.oauth2.auth.common.model.Scope;

import java.util.Date;
import java.util.Set;

/**
 * refresh_token 记录。refresh 是不透明随机串，元数据必须落库（或缓存）。
 * familyId 用于「滚动刷新」：同一令牌族共享 family，便于整族失效（如用户改密）。
 */
public class RefreshRecord {

    private final String token;
    private final String familyId;
    private final String userId;
    private final String clientId;
    private final Set<Scope> scopes;
    private final Date expiry;
    private boolean revoked;

    public RefreshRecord(String token, String familyId, String userId, String clientId,
                         Set<Scope> scopes, Date expiry) {
        this.token = token;
        this.familyId = familyId;
        this.userId = userId;
        this.clientId = clientId;
        this.scopes = scopes;
        this.expiry = expiry;
    }

    public String getToken() {
        return token;
    }

    public String getFamilyId() {
        return familyId;
    }

    public String getUserId() {
        return userId;
    }

    public String getClientId() {
        return clientId;
    }

    public Set<Scope> getScopes() {
        return scopes;
    }

    public Date getExpiry() {
        return expiry;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isExpired() {
        return expiry.before(new Date());
    }
}
