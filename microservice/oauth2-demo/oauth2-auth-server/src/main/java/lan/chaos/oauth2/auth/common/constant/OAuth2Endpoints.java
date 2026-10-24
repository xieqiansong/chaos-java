package lan.chaos.oauth2.auth.common.constant;

/** 集中引用端点路径常量，避免到处拼字符串。 */
public final class OAuth2Endpoints {

    private OAuth2Endpoints() {
    }

    public static final String AUTHORIZE = OAuth2Constant.AUTHORIZE_ENDPOINT;
    public static final String CONSENT = OAuth2Constant.CONSENT_ENDPOINT;
    public static final String TOKEN = OAuth2Constant.TOKEN_ENDPOINT;
    public static final String REVOKE = OAuth2Constant.REVOKE_ENDPOINT;
    public static final String INTROSPECT = OAuth2Constant.INTROSPECT_ENDPOINT;
    public static final String CLIENTS = OAuth2Constant.CLIENTS_ENDPOINT;
    public static final String JWKS = OAuth2Constant.JWKS_ENDPOINT;
    public static final String USERINFO = OAuth2Constant.USERINFO_ENDPOINT;
}
