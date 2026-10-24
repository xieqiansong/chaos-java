package lan.chaos.oauth2.auth.authorize;

import lan.chaos.oauth2.auth.common.model.Scope;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * 授权确认（consent）记录。把「用户是否批准此次授权」暂存起来，
 * 批准后才真正签发授权码。纯 API 形态下，UI 该渲染的 scope 文案由 {@link ConsentService} 生成。
 */
public class ConsentRecord {

    private final String consentId;
    private final String clientId;
    private final String userId;
    private final Set<Scope> scopes;
    private final String redirectUri;
    private final String state;
    private final String codeChallenge;
    private final Date expiry;
    private boolean decided;
    private boolean approved;

    public ConsentRecord(String clientId, String userId, Set<Scope> scopes,
                         String redirectUri, String state, String codeChallenge, Date expiry) {
        this.consentId = UUID.randomUUID().toString().replace("-", "");
        this.clientId = clientId;
        this.userId = userId;
        this.scopes = scopes;
        this.redirectUri = redirectUri;
        this.state = state;
        this.codeChallenge = codeChallenge;
        this.expiry = expiry;
    }

    public String getConsentId() {
        return consentId;
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

    public String getState() {
        return state;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public Date getExpiry() {
        return expiry;
    }

    public boolean isDecided() {
        return decided;
    }

    public void setDecided(boolean decided) {
        this.decided = decided;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isExpired() {
        return expiry.before(new Date());
    }
}
