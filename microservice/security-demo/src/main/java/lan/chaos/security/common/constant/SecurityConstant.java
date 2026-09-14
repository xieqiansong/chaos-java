package lan.chaos.security.common.constant;

/**
 * 认证授权演示的常量：路径、请求头、签名密钥。杜绝魔法值。
 */
public final class SecurityConstant {

    private SecurityConstant() {}

    /** 免认证路径。 */
    public static final String PUBLIC = "/api/public";
    /** 登录获取 JWT 的路径。 */
    public static final String TOKEN = "/api/token";
    /** 需 Basic 认证的路径（演示 Session/Cookie 之外的 HTTP 基础认证）。 */
    public static final String SECURE = "/api/secure";
    /** 需 Bearer JWT 的路径（演示无状态 Token 方案）。 */
    public static final String JWT_SECURE = "/api/jwt-secure";

    /** Authorization 头名。 */
    public static final String AUTH_HEADER = "Authorization";
    /** Bearer 前缀。 */
    public static final String BEARER = "Bearer ";

    /**
     * JWT 签名密钥（HS256 要求 >= 256 位）。生产应放配置中心/KMS，绝硬编码。
     * 这里固定 32 字节串仅为演示，长度满足算法要求。
     */
    public static final String JWT_SECRET = "chaos-demo-secret-key-please-replace-32-bytes";
}
