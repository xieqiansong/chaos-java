package lan.chaos.microservice.auth.service.impl;

import lan.chaos.microservice.auth.model.LoginRequest;
import lan.chaos.microservice.auth.model.LoginResponse;
import lan.chaos.microservice.auth.service.AuthService;
import lan.chaos.microservice.auth.service.RefreshTokenStore;
import lan.chaos.microservice.auth.service.UserStore;
import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.common.security.properties.JwtProperties;
import lan.chaos.microservice.common.security.util.JwtProvider;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 鉴权服务实现（P4 认证核心）。
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>双令牌</b>：access 短命无状态（网关验签即用），refresh 长命存服务端（可吊销）。
 *       既享 JWT「无状态」红利，又补上「能主动登出」的短板。</li>
 *   <li><b>密码仅做演示比对</b>：生产必须用 BCrypt/Argon2 加盐哈希，且用户表独立存储，绝不进 JWT。</li>
 *   <li><b>refresh 吊销</b>：refresh 的 jti 存 {@link RefreshTokenStore}，登出/风控时删除，
 *       即使旧 refresh 被截获也无法再用；access 过期后必须重新登录。</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private JwtProvider jwtProvider;

    @Resource
    private JwtProperties jwtProperties;

    @Resource
    private RefreshTokenStore refreshTokenStore;

    @Override
    public LoginResponse login(LoginRequest request) {
        UserStore.Credential credential = UserStore.findByUsername(request.getUsername());
        // 统一返回 401，不区分「用户不存在 / 密码错」，避免被枚举用户名
        if (credential == null || !credential.getPassword().equals(request.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        LoginUser user = credential.getLoginUser();
        return issueTokens(user);
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        // 1) refresh token 必须合法，且「不是 access」（防止拿 access 当 refresh 用）
        if (!jwtProvider.validateToken(refreshToken) || jwtProvider.isAccessToken(refreshToken)) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "refresh token 无效或已过期");
        }
        LoginUser user = jwtProvider.getLoginUser(refreshToken);
        String jti = jwtProvider.getJti(refreshToken);
        // 2) 服务端吊销校验：refresh 必须仍存在（登出后会被删除）
        if (!refreshTokenStore.exists(user.getUserId(), jti)) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "refresh token 已失效，请重新登录");
        }
        // 3) 重新签发 access（refresh 不变，演示「滑动会话」的最简形态）
        return issueTokens(user);
    }

    @Override
    public void logout(Long userId) {
        refreshTokenStore.removeAll(userId);
    }

    /** 签发双令牌并装配响应（输入→输出可观察）。 */
    private LoginResponse issueTokens(LoginUser user) {
        String access = jwtProvider.generateAccessToken(user);
        String refresh = jwtProvider.generateRefreshToken(user);
        // refresh 存服务端：key=用户+jti，TTL 与 refresh 过期一致（毫秒→秒）
        refreshTokenStore.save(user.getUserId(), jwtProvider.getJti(refresh),
                jwtProperties.getRefreshTokenTtl() / 1000);

        long expiresInSeconds = jwtProperties.getAccessTokenTtl() / 1000;
        return new LoginResponse(access, refresh, expiresInSeconds,
                user.getUserId(), user.getUsername(),
                new java.util.ArrayList<>(user.getPermissions()));
    }
}
