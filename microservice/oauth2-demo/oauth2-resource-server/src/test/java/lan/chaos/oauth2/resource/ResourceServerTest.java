package lan.chaos.oauth2.resource;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 资源服务器端到端测试（自包含，不拉外部 JWKS）。
 * 用测试 RSA 密钥签发 JWT，并覆写 JwtDecoder 为对应公钥，验证 200/401/403。
 */
@SpringBootTest(classes = {ResourceServerApplication.class, ResourceServerTest.TestJwtDecoderConfig.class})
@AutoConfigureMockMvc
class ResourceServerTest {

    private static RSAKey validKey;
    private static RSAKey otherKey;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void initKeys() throws Exception {
        validKey = new RSAKeyGenerator(2048).keyID("test-kid").generate();
        otherKey = new RSAKeyGenerator(2048).keyID("other-kid").generate();
    }

    /** 用指定密钥签发 JWT。 */
    private String sign(RSAKey key, String scope) throws Exception {
        JWSSigner signer = new RSASSASigner(key);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("alice")
                .claim("scope", scope)
                .claim("client_id", "demo_client_01")
                .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(signer);
        return jwt.serialize();
    }

    @Test
    void validToken_withScope_returns200() throws Exception {
        String token = sign(validKey, "user.basic");
        mockMvc.perform(get("/api/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongSignature_returns401() throws Exception {
        // 用 otherKey 签发（与解码公钥不符）→ 验签失败
        String token = sign(otherKey, "user.basic");
        mockMvc.perform(get("/api/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingScope_returns403() throws Exception {
        String token = sign(validKey, "user.email");
        mockMvc.perform(get("/api/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    /** 覆写 JwtDecoder：用测试公钥，避免依赖外部 JWKS 端点。 */
    @Configuration
    static class TestJwtDecoderConfig {
        @Bean
        public JwtDecoder jwtDecoder() throws Exception {
            return NimbusJwtDecoder.withPublicKey(validKey.toRSAPublicKey()).build();
        }
    }
}
