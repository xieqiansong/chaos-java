package lan.chaos.oauth2.auth;

import lan.chaos.oauth2.auth.common.util.PkceUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 授权服务器端到端集成测试（零外部依赖，内存存储，@SpringBootTest 自包含）。
 * 覆盖：授权码 happy path、refresh 滚动刷新、撤销、PKCE 公共客户端、consent 步骤、参数校验。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "oauth2.access-token-ttl-seconds=1800"
})
class AuthServerFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private String base() {
        return "http://localhost:" + port;
    }

    private HttpHeaders userBasic() {
        return basic("alice", "secret");
    }

    private HttpHeaders clientBasic() {
        return basic("demo_client_01", "demo_secret_01");
    }

    private HttpHeaders publicClientBasic() {
        // 公共客户端无 secret：Basic 仅 client_id，密码留空
        return basic("demo_public_client_01", "");
    }

    private HttpHeaders basic(String id, String secret) {
        HttpHeaders h = new HttpHeaders();
        String raw = id + ":" + secret;
        h.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.US_ASCII)));
        return h;
    }

    private Map<String, Object> authorize(String clientId, String scope, String redirectUri,
                                          boolean autoConsent, String codeChallenge) {
        StringBuilder url = new StringBuilder(base() + "/oauth2/authorize")
                .append("?client_id=").append(clientId)
                .append("&redirect_uri=").append(redirectUri)
                .append("&response_mode=json")
                .append("&auto_consent=").append(autoConsent);
        if (scope != null) {
            url.append("&scope=").append(scope);
        }
        if (codeChallenge != null) {
            url.append("&code_challenge=").append(codeChallenge).append("&code_challenge_method=S256");
        }
        ResponseEntity<Map> resp = rest.exchange(url.toString(), HttpMethod.GET,
                new HttpEntity<>(userBasic()), Map.class);
        return resp.getBody();
    }

    private Map<String, Object> exchangeCode(String code, String redirectUri, HttpHeaders clientHeaders,
                                            String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        if (codeVerifier != null) {
            form.add("code_verifier", codeVerifier);
        }
        HttpHeaders h = new HttpHeaders(clientHeaders);
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> resp = rest.postForEntity(base() + "/oauth2/token",
                new HttpEntity<>(form, h), Map.class);
        return resp.getBody();
    }

    @Test
    @SuppressWarnings("unchecked")
    void happyPath_authorizeTokenUserInfoIntrospect() {
        Map<String, Object> authz = authorize("demo_client_01", "user.basic", "http://127.0.0.1:18083/cb", true, null);
        assertThat(authz).containsKey("code");

        Map<String, Object> tokens = exchangeCode((String) authz.get("code"), "http://127.0.0.1:18083/cb", clientBasic(), null);
        String access = (String) tokens.get("access_token");
        String refresh = (String) tokens.get("refresh_token");
        assertThat(access).isNotBlank();
        assertThat(refresh).startsWith("rft_");
        assertThat(tokens.get("token_type")).isEqualTo("Bearer");
        assertThat(tokens.get("scope")).isEqualTo("user.basic");

        // 用 access_token 访问 auth-server 的 /api/userinfo
        HttpHeaders bearer = new HttpHeaders();
        bearer.set("Authorization", "Bearer " + access);
        ResponseEntity<Map> userinfo = rest.exchange(base() + "/api/userinfo", HttpMethod.GET,
                new HttpEntity<>(bearer), Map.class);
        assertThat(userinfo.getStatusCodeValue()).isEqualTo(200);
        assertThat(userinfo.getBody().get("sub")).isEqualTo("alice");

        // 自省：active=true
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", access);
        HttpHeaders h = new HttpHeaders(clientBasic());
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> intro = rest.postForEntity(base() + "/oauth2/introspect",
                new HttpEntity<>(form, h), Map.class);
        assertThat(intro.getBody().get("active")).isEqualTo(Boolean.TRUE);
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshToken_rollsAndInvalidatesOld() {
        Map<String, Object> authz = authorize("demo_client_01", "user.basic", "http://127.0.0.1:18083/cb", true, null);
        Map<String, Object> tokens = exchangeCode((String) authz.get("code"), "http://127.0.0.1:18083/cb", clientBasic(), null);
        String oldRefresh = (String) tokens.get("refresh_token");

        // 用 refresh_token 刷新 → 拿到新令牌
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", oldRefresh);
        HttpHeaders h = new HttpHeaders(clientBasic());
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> refreshed = rest.postForEntity(base() + "/oauth2/token",
                new HttpEntity<>(form, h), Map.class);
        assertThat((String) refreshed.getBody().get("access_token")).isNotBlank();
        assertThat((String) refreshed.getBody().get("refresh_token")).isNotBlank();

        // 旧 refresh 再次使用 → 应失败（滚动刷新使其失效）
        ResponseEntity<Map> reuse = rest.postForEntity(base() + "/oauth2/token",
                new HttpEntity<>(form, h), Map.class);
        assertThat(reuse.getStatusCodeValue()).isEqualTo(400);
        assertThat((Map<String, Object>) reuse.getBody()).containsEntry("error", "invalid_grant");
    }

    @Test
    @SuppressWarnings("unchecked")
    void revoke_accessToken_makesItInactive() {
        Map<String, Object> authz = authorize("demo_client_01", "user.basic", "http://127.0.0.1:18083/cb", true, null);
        Map<String, Object> tokens = exchangeCode((String) authz.get("code"), "http://127.0.0.1:18083/cb", clientBasic(), null);
        String access = (String) tokens.get("access_token");

        // 撤销 access_token
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", access);
        form.add("token_type_hint", "access_token");
        HttpHeaders h = new HttpHeaders(clientBasic());
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Void> revoke = rest.postForEntity(base() + "/oauth2/revoke",
                new HttpEntity<>(form, h), Void.class);
        assertThat(revoke.getStatusCodeValue()).isEqualTo(200);

        // 自省应返回 active=false
        MultiValueMap<String, String> introForm = new LinkedMultiValueMap<>();
        introForm.add("token", access);
        ResponseEntity<Map> intro = rest.postForEntity(base() + "/oauth2/introspect",
                new HttpEntity<>(introForm, h), Map.class);
        assertThat(intro.getBody().get("active")).isEqualTo(Boolean.FALSE);

        // 用被撤销的 token 访问 userinfo → 401
        HttpHeaders bearer = new HttpHeaders();
        bearer.set("Authorization", "Bearer " + access);
        ResponseEntity<Map> userinfo = rest.exchange(base() + "/api/userinfo", HttpMethod.GET,
                new HttpEntity<>(bearer), Map.class);
        assertThat(userinfo.getStatusCodeValue()).isEqualTo(401);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pkce_publicClient_successAndFailure() {
        String verifier = PkceUtil.generateVerifier();
        String challenge = PkceUtil.challengeS256(verifier);

        Map<String, Object> authz = authorize("demo_public_client_01", "user.basic", "http://127.0.0.1:18083/cb", true, challenge);
        String code = (String) authz.get("code");
        assertThat(code).isNotBlank();

        // 正确 verifier → 成功
        Map<String, Object> tokens = exchangeCode(code, "http://127.0.0.1:18083/cb", publicClientBasic(), verifier);
        assertThat((String) tokens.get("access_token")).isNotBlank();

        // 错误 verifier → 400 invalid_grant
        Map<String, Object> authz2 = authorize("demo_public_client_01", "user.basic", "http://127.0.0.1:18083/cb", true, challenge);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", (String) authz2.get("code"));
        form.add("redirect_uri", "http://127.0.0.1:18083/cb");
        form.add("code_verifier", "wrong-verifier");
        HttpHeaders h = new HttpHeaders(publicClientBasic());
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> bad = rest.postForEntity(base() + "/oauth2/token",
                new HttpEntity<>(form, h), Map.class);
        assertThat(bad.getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    @SuppressWarnings("unchecked")
    void consentStep_requiresExplicitApproval() {
        // 不带 auto_consent → 返回 consent_id 与 scope 文案
        Map<String, Object> authz = authorize("demo_client_01", "user.basic", "http://127.0.0.1:18083/cb", false, null);
        String consentId = (String) authz.get("consent_id");
        assertThat(consentId).isNotBlank();
        assertThat((java.util.List<Map<String, String>>) authz.get("scopes")).isNotEmpty();

        // 批准 → 拿到 code
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("consent_id", consentId);
        form.add("decision", "approve");
        ResponseEntity<Map> approve = rest.postForEntity(base() + "/oauth2/consent",
                new HttpEntity<>(form, new HttpEntity<>(userBasic()).getHeaders()), Map.class);
        String code = (String) approve.getBody().get("code");
        assertThat(code).isNotBlank();

        // 用 code 换 token
        Map<String, Object> tokens = exchangeCode(code, "http://127.0.0.1:18083/cb", clientBasic(), null);
        assertThat((String) tokens.get("access_token")).isNotBlank();
    }

    @Test
    @SuppressWarnings("unchecked")
    void validation_invalidScopeAndRedirect() {
        // 超范围 scope → 400 invalid_scope
        ResponseEntity<Map> badScope = rest.exchange(
                base() + "/oauth2/authorize?client_id=demo_client_01&redirect_uri=http://127.0.0.1:18083/cb"
                        + "&scope=user.phone&response_mode=json&auto_consent=true",
                HttpMethod.GET, new HttpEntity<>(userBasic()), Map.class);
        assertThat(badScope.getStatusCodeValue()).isEqualTo(400);
        assertThat((Map<String, Object>) badScope.getBody()).containsEntry("error", "invalid_scope");

        // redirect_uri 不匹配 → 400 invalid_request
        ResponseEntity<Map> badRedirect = rest.exchange(
                base() + "/oauth2/authorize?client_id=demo_client_01&redirect_uri=http://evil.com/cb"
                        + "&response_mode=json&auto_consent=true",
                HttpMethod.GET, new HttpEntity<>(userBasic()), Map.class);
        assertThat(badRedirect.getStatusCodeValue()).isEqualTo(400);
        assertThat((Map<String, Object>) badRedirect.getBody()).containsEntry("error", "invalid_request");
    }

    @Test
    void jwks_exposesPublicKey() {
        ResponseEntity<Map> resp = rest.getForEntity(base() + "/.well-known/jwks.json", Map.class);
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat((java.util.List<Map<String, Object>>) resp.getBody().get("keys")).isNotEmpty();
    }
}
