package lan.chaos.microservice.gateway.filter;

import lan.chaos.microservice.common.security.constant.SecurityConstants;
import lan.chaos.microservice.common.security.util.JwtProvider;
import org.springframework.util.AntPathMatcher;

/**
 * 鉴权决策（纯逻辑，可单测）：把「白名单判定」「token 合法性判定」从 WebFlux 过滤器里抽出来。
 *
 * <p>P4 网关职责：所有请求先过网关，命中白名单（如 {@code /auth/**}）直接放行；
 * 其余必须带合法 access token，否则 401。下游服务只做细粒度权限（@RequiresPermission）。</p>
 */
public class AuthDecision {

    private final JwtProvider jwtProvider;

    private final AntPathMatcher matcher = new AntPathMatcher();

    public AuthDecision(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    /** 路径是否命中白名单（免鉴权）。 */
    public boolean isWhitelisted(String path) {
        for (String pattern : SecurityConstants.DEFAULT_WHITELIST) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 请求是否鉴权通过：头存在、Bearer 前缀、且为合法未过期的 access token。
     * 注意这里同时校验「是 access 而非 refresh」，防止拿 refresh 走业务接口。
     */
    public boolean authorized(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return false;
        }
        String token = authorizationHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
        return jwtProvider.validateToken(token) && jwtProvider.isAccessToken(token);
    }
}
