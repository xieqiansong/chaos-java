package lan.chaos.oauth2.auth.common.constant;

/**
 * OAuth2 端点路径、grant_type、TTL 等常量，杜绝魔法值。
 */
public final class OAuth2Constant {

    private OAuth2Constant() {
    }

    // 端点路径
    public static final String AUTHORIZE_ENDPOINT = "/oauth2/authorize";
    public static final String CONSENT_ENDPOINT = "/oauth2/consent";
    public static final String TOKEN_ENDPOINT = "/oauth2/token";
    public static final String REVOKE_ENDPOINT = "/oauth2/revoke";
    public static final String INTROSPECT_ENDPOINT = "/oauth2/introspect";
    public static final String CLIENTS_ENDPOINT = "/oauth2/clients";
    public static final String JWKS_ENDPOINT = "/.well-known/jwks.json";
    public static final String USERINFO_ENDPOINT = "/api/userinfo";

    // grant_type
    public static final String GRANT_AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_REFRESH_TOKEN = "refresh_token";

    // scope 分隔符（空格，OAuth2 规范）
    public static final String SCOPE_DELIMITER = " ";

    // 默认 TTL（秒）：授权码 10 分钟；access 30 分钟；refresh 14 天
    public static final long CODE_TTL_SECONDS = 600L;
    public static final long ACCESS_TOKEN_TTL_SECONDS = 1800L;
    public static final long REFRESH_TOKEN_TTL_SECONDS = 1_209_600L;

    // 生产约束提示：所有授权/Token 相关请求必须走 HTTPS
    public static final String PRODUCTION_HTTPS_NOTE = "PRODUCTION: 所有 /oauth2/* 与 /api/userinfo 请求必须走 HTTPS，防中间人攻击。";

    // 样例用户（HTTP Basic 登录授权服务器用）
    public static final String SAMPLE_USERNAME = "alice";
    public static final String SAMPLE_PASSWORD = "secret";
}
