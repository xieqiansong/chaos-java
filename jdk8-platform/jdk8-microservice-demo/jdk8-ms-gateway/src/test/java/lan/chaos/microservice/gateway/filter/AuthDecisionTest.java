package lan.chaos.microservice.gateway.filter;

import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.common.security.properties.JwtProperties;
import lan.chaos.microservice.common.security.util.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthDecision 单测（纯单元，不依赖 WebFlux 容器）。
 * 覆盖「白名单放行 / 合法 access 通过 / 缺头·前缀错·用 refresh·过期 均拒绝」。
 */
class AuthDecisionTest {

    private JwtProvider jwtProvider;

    private AuthDecision decision;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("REDACTED-abcdefghijklmnop");
        props.setAccessTokenTtl(30 * 60 * 1000L);
        jwtProvider = new JwtProvider(props);
        decision = new AuthDecision(jwtProvider);
    }

    @Test
    void whitelist_allowsAuthAndActuator() {
        assertTrue(decision.isWhitelisted("/auth/login"));
        assertTrue(decision.isWhitelisted("/auth/refresh"));
        assertTrue(decision.isWhitelisted("/actuator/health"));
        assertFalse(decision.isWhitelisted("/users/1"));
        assertFalse(decision.isWhitelisted("/orders/tx"));
    }

    @Test
    void authorized_withValidAccessToken() {
        LoginUser user = new LoginUser(1L, "admin", Collections.singleton("user:read"));
        String token = jwtProvider.generateAccessToken(user);
        assertTrue(decision.authorized("Bearer " + token));
    }

    @Test
    void unauthorized_whenMissingOrWrongPrefix() {
        assertFalse(decision.authorized(null));
        assertFalse(decision.authorized(""));
        assertFalse(decision.authorized("Basic abcdef"));
    }

    @Test
    void unauthorized_whenUsingRefreshToken() {
        LoginUser user = new LoginUser(1L, "admin", Collections.singleton("user:read"));
        String refresh = jwtProvider.generateRefreshToken(user);
        assertFalse(decision.authorized("Bearer " + refresh));
    }

    @Test
    void unauthorized_whenExpired() {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("REDACTED-abcdefghijklmnop");
        shortLived.setAccessTokenTtl(-1000L);
        JwtProvider p = new JwtProvider(shortLived);
        LoginUser user = new LoginUser(1L, "admin", Collections.singleton("user:read"));
        String expired = p.generateAccessToken(user);
        assertFalse(decision.authorized("Bearer " + expired));
    }
}
