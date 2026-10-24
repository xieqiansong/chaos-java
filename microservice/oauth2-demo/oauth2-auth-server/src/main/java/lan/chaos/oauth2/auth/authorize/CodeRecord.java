package lan.chaos.oauth2.auth.authorize;

import lan.chaos.oauth2.auth.common.model.Scope;

import java.util.Date;
import java.util.Set;

/**
 * 授权码记录。授权码的关键属性：
 * - 一次性（consumed 后作废）
 * - 短生命周期（默认 10 分钟）
 * - 绑定客户端/用户/scope/PKCE challenge，防止被挪用到别的客户端
 */
public class CodeRecord {

    private final String code;
    private final String clientId;
    private final String userId;
    private final Set<Scope> scopes;
    private final String redirectUri;
    private final String codeChallenge;
    private final Date expiry;
    private boolean consumed;

    public CodeRecord(String code, String clientId, String userId, Set<Scope> scopes,
                      String redirectUri, String codeChallenge, Date expiry) {
        this.code = code;
        this.clientId = clientId;
        this.userId = userId;
        this.scopes = scopes;
        this.redirectUri = redirectUri;
        this.codeChallenge = codeChallenge;
        this.expiry = expiry;
    }

    public String getCode() {
        return code;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserId() {
        return userId;
    }

    public Set<Scope> getScopes() {
        return scopes;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public Date getExpiry() {
        return expiry;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public boolean isExpired() {
        return expiry.before(new Date());
    }
}
