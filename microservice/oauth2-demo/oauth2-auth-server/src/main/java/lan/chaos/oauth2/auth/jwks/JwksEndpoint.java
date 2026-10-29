package lan.chaos.oauth2.auth.jwks;

import lan.chaos.oauth2.auth.common.constant.OAuth2Endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWK 公钥分发端点（能力⑥）。资源服务器配置 jwk-set-uri 指向此处即可拉取公钥验签，
 * 无需共享私钥。呼应 security-demo 的「OAuth2 资源服务器（外部 IdP）」场景——本服务即那个 IdP。
 */
@RestController
public class JwksEndpoint {

    private final RSAPublicKey publicKey;
    private final String kid;

    public JwksEndpoint(RSAPublicKey publicKey, String kid) {
        this.publicKey = publicKey;
        this.kid = kid;
    }

    @GetMapping(OAuth2Endpoints.JWKS)
    public ResponseEntity<Map<String, Object>> jwks() {
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", kid);
        jwk.put("n", base64Url(publicKey.getModulus()));
        jwk.put("e", base64Url(publicKey.getPublicExponent()));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("keys", Collections.singletonList(jwk));
        return ResponseEntity.ok(response);
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        // 去掉可能的符号位前导 0，保证无符号大端
        if (bytes[0] == 0) {
            byte[] stripped = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, stripped, 0, stripped.length);
            bytes = stripped;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
