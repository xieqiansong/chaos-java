package lan.chaos.oauth2.auth.client;

import java.util.List;

/** 客户端注册请求（演示用，生产需管理员审核 + 限制 redirect_uri 安全性）。 */
public class ClientRegistrationRequest {

    private String clientName;
    private List<String> redirectUris;
    private List<String> scopes;
    private boolean pkceRequired;
    private List<String> grantTypes;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public boolean isPkceRequired() {
        return pkceRequired;
    }

    public void setPkceRequired(boolean pkceRequired) {
        this.pkceRequired = pkceRequired;
    }

    public List<String> getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grantTypes = grantTypes;
    }
}
