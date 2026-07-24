package lan.chaos.microservice.common.security.constant;

/**
 * 安全相关常量（全 demo 统一，避免魔法值）。
 *
 * <p>网关、鉴权服务、下游业务服务都引用这里，保证「头名 / token 前缀 / 白名单 / claim 键名」一致。</p>
 */
public final class SecurityConstants {

    /** 认证头名（Authorization），与下游透传、Feign 透传保持一致。 */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer token 前缀（注意后面有空格，截取时一并去掉）。 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** JWT claim 键：用户 ID。 */
    public static final String CLAIM_USER_ID = "uid";

    /** JWT claim 键：用户名。 */
    public static final String CLAIM_USERNAME = "uname";

    /** JWT claim 键：权限集合（逗号分隔的字符串，避免写入 List 的序列化兼容问题）。 */
    public static final String CLAIM_PERMISSIONS = "perms";

    /** JWT claim 键：令牌类型（access / refresh），用于校验「拿 refresh 当 access 用」的越权。 */
    public static final String CLAIM_TOKEN_TYPE = "typ";

    public static final String TOKEN_TYPE_ACCESS = "access";

    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * 默认白名单：网关/拦截器对命中这些路径放行，不做鉴权。
     * <p>设计：认证接口本身必须放行（否则死循环），监控/静态资源也放行。</p>
     */
    public static final String[] DEFAULT_WHITELIST = {
            "/auth/**",
            "/actuator/**",
            "/favicon.ico",
            "/error",
            "/*.html",
            "/*.js",
            "/*.css",
            "/webjars/**"
    };

    private SecurityConstants() {
    }
}
