package lan.chaos.oauth2.auth.token;

import lan.chaos.oauth2.auth.common.model.Scope;

import java.util.Date;
import java.util.Set;

/**
 * access_token（JWT）的元数据记录。即使 JWT 自包含，仍需在授权服务器侧登记，
 * 以支持「主动撤销（黑名单）」与「按用户/客户端批量撤销」。
 */
public class AccessRecord {

    private final String jti;
    private final String userId;
    private final String clientId;
    private final Set<Scope> scopes;
    private final Date expiry;
    private boolean revoked;

    public AccessRecord(String jti, String userId, String clientId, Set<Scope> scopes, Date expiry) {
        this.jti = jti;
        this.userId = userId;
        this.clientId = clientId;
        this.scopes = scopes;
        this.expiry = expiry;
    }

    public String getJti() {
        return jti;
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
