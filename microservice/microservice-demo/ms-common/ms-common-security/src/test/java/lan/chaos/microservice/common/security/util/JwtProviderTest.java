package lan.chaos.microservice.common.security.util;

import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.common.security.properties.JwtProperties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtProvider 单测：覆盖「签发→校验」「claims 还原」「过期」「篡改」四个核心语义。
 * 不依赖任何中间件，离线可跑。
 */
class JwtProviderTest {

    private JwtProvider newProvider() {
        JwtProperties props = new JwtProperties();
        props.setSecret("chaos-demo-secret-key-please-replace-32-bytes");
        props.setAccessTokenTtl(30 * 60 * 1000L);
        return new JwtProvider(props);
    }

    private LoginUser sampleUser() {
        Set<String> perms = new HashSet<>(Arrays.asList("user:read", "user:write", "order:read"));
        return new LoginUser(1L, "admin", perms);
    }

    @Test
    void roundTrip_accessToken_parsesBackToSameUser() {
        JwtProvider provider = newProvider();
        String token = provider.generateAccessToken(sampleUser());

        assertTrue(provider.validateToken(token));
        assertTrue(provider.isAccessToken(token));
        assertFalse(provider.isAccessToken(provider.generateRefreshToken(sampleUser())));

        LoginUser parsed = provider.getLoginUser(token);
        assertEquals(1L, parsed.getUserId());
        assertEquals("admin", parsed.getUsername());
        assertTrue(parsed.hasPermission("user:write"));
        assertTrue(parsed.hasPermission("order:read"));
        assertFalse(parsed.hasPermission("order:write"));
    }

    @Test
    void expiredToken_isInvalid() {
        JwtProvider provider = newProvider();
        // 把过期时间设为负数 → 签发时 exp 已早于 now → 校验失败
        JwtProperties props = new JwtProperties();
        props.setSecret("chaos-demo-secret-key-please-replace-32-bytes");
        props.setAccessTokenTtl(-1000L);
        JwtProvider shortLived = new JwtProvider(props);

        String token = shortLived.generateAccessToken(sampleUser());
        assertFalse(shortLived.validateToken(token));
    }

    @Test
    void tamperedToken_isInvalid() {
        JwtProvider provider = newProvider();
        String token = provider.generateAccessToken(sampleUser());
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

        assertFalse(provider.validateToken(tampered));
    }

    @Test
    void refreshToken_carriesJti() {
        JwtProvider provider = newProvider();
        String refresh = provider.generateRefreshToken(sampleUser());

        assertTrue(provider.validateToken(refresh));
        // jti 非空且可解析
        assertFalse(provider.getJti(refresh).isEmpty());
    }

    @Test
    void parse_wrongSecret_throws() {
        JwtProvider providerA = newProvider();
        JwtProperties propsB = new JwtProperties();
        propsB.setSecret("another-secret-another-secret-another-secret-another-sec");
        JwtProvider providerB = new JwtProvider(propsB);

        String token = providerA.generateAccessToken(sampleUser());
        assertThrows(RuntimeException.class, () -> providerB.parse(token));
    }
}
