package lan.chaos.oauth2.auth.client;

import java.util.Set;

/** 注册结果：client_id 持久存在，rawSecret 仅此一次返回（生产应提示妥善保存）。 */
public class ClientRegistrationResult {

    private final String clientId;
    private final String rawSecret;
    private final String clientName;
    private final Set<String> redirectUris;
    private final Set<String> scopes;
    private final boolean pkceRequired;

    public ClientRegistrationResult(String clientId, String rawSecret, String clientName,
                                   Set<String> redirectUris, Set<String> scopes, boolean pkceRequired) {
        this.clientId = clientId;
        this.rawSecret = rawSecret;
        this.clientName = clientName;
        this.redirectUris = redirectUris;
        this.scopes = scopes;
        this.pkceRequired = pkceRequired;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRawSecret() {
        return rawSecret;
    }

    public String getClientName() {
        return clientName;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public boolean isPkceRequired() {
        return pkceRequired;
    }
}
