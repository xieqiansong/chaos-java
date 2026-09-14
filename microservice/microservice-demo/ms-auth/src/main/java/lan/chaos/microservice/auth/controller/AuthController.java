package lan.chaos.microservice.auth.controller;

import lan.chaos.microservice.auth.model.LoginRequest;
import lan.chaos.microservice.auth.model.LoginResponse;
import lan.chaos.microservice.auth.service.AuthService;
import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.common.security.constant.SecurityConstants;
import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.common.security.util.JwtProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 鉴权 HTTP 入口（仅做参数接收 + 委托 AuthService，不含业务知识点）。
 *
 * <p>三个端点都在白名单 {@code /auth/**} 内，网关不拦截，可直接 curl 把玩：</p>
 * <pre>
 *   POST /auth/login?username=admin&password=admin123   → 拿到 accessToken / refreshToken
 *   POST /auth/refresh?refreshToken=xxx                  → 换新 accessToken
 *   POST /auth/logout  （带 Authorization: Bearer access）→ 吊销该用户 refresh，踢下线
 * </pre>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private JwtProvider jwtProvider;

    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@RequestParam("refreshToken") String refreshToken) {
        return R.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = SecurityConstants.AUTHORIZATION_HEADER, required = false) String authHeader) {
        LoginUser user = resolveUser(authHeader);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "未认证，无法登出");
        }
        authService.logout(user.getUserId());
        return R.ok();
    }

    private LoginUser resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return null;
        }
        String token = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
        if (!jwtProvider.validateToken(token)) {
            return null;
        }
        return jwtProvider.getLoginUser(token);
    }
}
