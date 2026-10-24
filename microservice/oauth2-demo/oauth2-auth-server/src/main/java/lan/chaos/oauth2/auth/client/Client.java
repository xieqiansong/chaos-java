package lan.chaos.oauth2.auth.client;

import lan.chaos.oauth2.auth.common.model.Scope;

import java.util.HashSet;
import java.util.Set;

/**
 * 第三方应用（OAuth2 客户端）注册信息。
 *
 * <p>关键点：client_secret 只以 bcrypt 哈希形式保存（{@link #clientSecretHash}），
 * 明文 secret 仅在注册返回时一次性给到调用方。</p>
 */
public class Client {

    private String clientId;
    /** bcrypt 哈希；公共客户端（pkceRequired=true）为 null，靠 PKCE 保证安全。 */
    private String clientSecretHash;
    private String clientName;
    private final Set<String> redirectUris = new HashSet<>();
    private final Set<Scope> scopes = new HashSet<>();
    private final Set<String> grantTypes = new HashSet<>();
    /** 是否强制 PKCE（公共客户端应置 true）。 */
    private boolean pkceRequired;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public void setClientSecretHash(String clientSecretHash) {
        this.clientSecretHash = clientSecretHash;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public Set<Scope> getScopes() {
        return scopes;
    }

    public Set<String> getGrantTypes() {
        return grantTypes;
    }

    public boolean isPkceRequired() {
        return pkceRequired;
    }

    public void setPkceRequired(boolean pkceRequired) {
        this.pkceRequired = pkceRequired;
    }
}
