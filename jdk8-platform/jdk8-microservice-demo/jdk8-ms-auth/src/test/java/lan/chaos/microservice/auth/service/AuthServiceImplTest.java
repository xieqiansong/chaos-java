package lan.chaos.microservice.auth.service;

import lan.chaos.microservice.auth.model.LoginRequest;
import lan.chaos.microservice.auth.model.LoginResponse;
import lan.chaos.microservice.auth.service.impl.AuthServiceImpl;
import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.security.properties.JwtProperties;
import lan.chaos.microservice.common.security.util.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthService 单测（纯单元，不连 Redis/NACOS）。用 {@link InMemoryRefreshTokenStore} 兜底，
 * 通过反射注入依赖，覆盖「登录成功/失败、刷新成功、登出后刷新失效、非法 refresh」五个语义。
 */
class AuthServiceImplTest {

    private JwtProvider jwtProvider;

    private InMemoryRefreshTokenStore store;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("REDACTED-abcdefghijklmnop");
        props.setAccessTokenTtl(30 * 60 * 1000L);
        props.setRefreshTokenTtl(7 * 24 * 60 * 60 * 1000L);
        jwtProvider = new JwtProvider(props);
        store = new InMemoryRefreshTokenStore();
        authService = new AuthServiceImpl();
        // @Resource 字段在纯单测里无容器注入，用 ReflectionTestUtils 手动装配
        ReflectionTestUtils.setField(authService, "jwtProvider", jwtProvider);
        ReflectionTestUtils.setField(authService, "jwtProperties", props);
        ReflectionTestUtils.setField(authService, "refreshTokenStore", store);
    }

    @Test
    void login_success_issuesTokensAndStoresRefresh() {
        LoginResponse resp = authService.login(new LoginRequest("admin", "admin123"));

        assertNotNull(resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
        assertEquals("admin", resp.getUsername());
        assertTrue(resp.getPermissions().contains("user:write"));

        // refresh 已落库（可吊销）
        String jti = jwtProvider.getJti(resp.getRefreshToken());
        assertTrue(store.exists(1L, jti));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        BizException ex = assertThrows(BizException.class,
                () -> authService.login(new LoginRequest("admin", "wrong")));
        assertEquals(401, ex.getCode());
    }

    @Test
    void refresh_success_returnsNewAccessToken() {
        LoginResponse login = authService.login(new LoginRequest("guest", "guest123"));
        LoginResponse refreshed = authService.refresh(login.getRefreshToken());

        assertNotNull(refreshed.getAccessToken());
        assertTrue(jwtProvider.validateToken(refreshed.getAccessToken()));
        // guest 只有 user:read
        assertTrue(refreshed.getPermissions().contains("user:read"));
    }

    @Test
    void refresh_afterLogout_isRejected() {
        LoginResponse login = authService.login(new LoginRequest("admin", "admin123"));
        authService.logout(1L);

        // 登出后 refresh 已从存储删除，再刷新应被拒
        BizException ex = assertThrows(BizException.class,
                () -> authService.refresh(login.getRefreshToken()));
        assertEquals(401, ex.getCode());
    }

    @Test
    void refresh_withAccessToken_isRejected() {
        LoginResponse login = authService.login(new LoginRequest("admin", "admin123"));
        // 拿 access 当 refresh 用，应被拒
        assertThrows(BizException.class, () -> authService.refresh(login.getAccessToken()));
    }
}
