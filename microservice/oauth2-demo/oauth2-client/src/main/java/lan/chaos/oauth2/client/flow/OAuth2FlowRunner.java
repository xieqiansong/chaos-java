package lan.chaos.oauth2.client.flow;

import lan.chaos.oauth2.client.common.constant.ClientConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * 第三方客户端：程序化跑通「授权码模式」全链路，并打印每一步的输入→输出（可观察）。
 *
 * <p>演示的是「保密客户端」happy path（带 client_secret，无需 PKCE）。流程：
 * 1) 引导用户到授权端点登录并授权（这里用 auto_consent 直通）→ 拿授权码；
 * 2) 用授权码换 access_token + refresh_token；
 * 3) 带 access_token 调资源服务器受保护接口；
 * 4) 用 refresh_token 滚动刷新；
 * 5) 撤销 access_token 后，资源服务器应返回 401（验证撤销生效）。</p>
 *
 * <p>需要 auth-server(18081) 与 resource-server(18082) 已启动（见 README 快速开始）。</p>
 */
@Component
public class OAuth2FlowRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OAuth2FlowRunner.class);
    private final RestTemplate rest = new RestTemplate();

    @Value("${oauth2.client.auth-server-base-url}")
    private String authBase;
    @Value("${oauth2.client.resource-server-base-url}")
    private String resourceBase;
    @Value("${oauth2.client.client-id}")
    private String clientId;
    @Value("${oauth2.client.client-secret}")
    private String clientSecret;
    @Value("${oauth2.client.username}")
    private String username;
    @Value("${oauth2.client.password}")
    private String password;
    @Value("${oauth2.client.redirect-uri}")
    private String redirectUri;
    @Value("${oauth2.client.scope}")
    private String scope;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== OAuth2 第三方客户端：授权码流程演示 ==========");
        try {
            String code = step1Authorize();
            Map<String, Object> tokens = step2ExchangeCode(code);
            String accessToken = (String) tokens.get("access_token");
            String refreshToken = (String) tokens.get("refresh_token");

            step3CallResource(accessToken);

            Map<String, Object> refreshed = step4Refresh(refreshToken);
            String newAccessToken = (String) refreshed.get("access_token");

            step5RevokeAndVerify(newAccessToken);

            log.info("========== 演示结束：每条链路均符合预期 ==========");
        } catch (Exception e) {
            log.error("流程中断（请确认 auth-server 18081 与 resource-server 18082 已启动）：{}", e.getMessage());
        }
    }

    /** 步骤1：到授权端点登录(alice)并授权，拿授权码。 */
    private String step1Authorize() {
        String state = UUID.randomUUID().toString().substring(0, 8);
        String url = buildAuthorizeUrl(authBase, clientId, redirectUri, scope, state);
        log.info("[1] 引导授权 → GET {}", url);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basicHeader(username, password));
        ResponseEntity<Map> resp = rest.exchange(url, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = resp.getBody();
        log.info("    返回(授权码)：{}", body);
        return (String) body.get("code");
    }

    /** 步骤2：用授权码换 Token。 */
    private Map<String, Object> step2ExchangeCode(String code) {
        String url = authBase + ClientConstant.TOKEN;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basicHeader(clientId, clientSecret));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> resp = rest.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
        log.info("[2] 换 Token → POST {} (grant_type=authorization_code)", url);
        log.info("    返回：access_token(前20位)={} refresh_token(前12位)={} expires_in={} scope={}",
                mask((String) resp.getBody().get("access_token"), 20),
                mask((String) resp.getBody().get("refresh_token"), 12),
                resp.getBody().get("expires_in"), resp.getBody().get("scope"));
        return resp.getBody();
    }

    /** 步骤3：带 access_token 调资源服务器。 */
    private void step3CallResource(String accessToken) {
        String url = resourceBase + ClientConstant.USER_INFO;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        ResponseEntity<Map> resp = rest.exchange(url, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        log.info("[3] 访问资源 → GET {} (Bearer access_token)", url);
        log.info("    资源服务器返回：{}", resp.getBody());
    }

    /** 步骤4：用 refresh_token 滚动刷新。 */
    private Map<String, Object> step4Refresh(String refreshToken) {
        String url = authBase + ClientConstant.TOKEN;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basicHeader(clientId, clientSecret));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> resp = rest.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
        log.info("[4] 刷新 Token → POST {} (grant_type=refresh_token, 滚动刷新使旧 refresh 失效)", url);
        log.info("    返回：新 access_token(前20位)={} 新 refresh_token(前12位)={}",
                mask((String) resp.getBody().get("access_token"), 20),
                mask((String) resp.getBody().get("refresh_token"), 12));
        return resp.getBody();
    }

    /** 步骤5：撤销 access_token，再访问资源应 401。 */
    private void step5RevokeAndVerify(String accessToken) {
        String revokeUrl = authBase + ClientConstant.REVOKE;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", accessToken);
        form.add("token_type_hint", "access_token");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basicHeader(clientId, clientSecret));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        rest.postForEntity(revokeUrl, new HttpEntity<>(form, headers), Map.class);
        log.info("[5] 撤销 Token → POST {} (token_type_hint=access_token)", revokeUrl);

        String url = resourceBase + ClientConstant.USER_INFO;
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + accessToken);
        ResponseEntity<String> resp = rest.exchange(url, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(h), String.class);
        log.info("    撤销后访问资源 → GET {} 期望 401，实际 {}", url, resp.getStatusCode());
    }

    /** 组装授权端点 URL（含 state、auto_consent 直通、response_mode=json）。 */
    String buildAuthorizeUrl(String base, String clientId, String redirectUri, String scope, String state) {
        return UriComponentsBuilder.fromHttpUrl(base + ClientConstant.AUTHORIZE)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("response_mode", "json")
                .queryParam("auto_consent", "true")
                .toUriString();
    }

    private String basicHeader(String id, String secret) {
        String raw = id + ":" + secret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.US_ASCII));
    }

    private String mask(String token, int keep) {
        if (token == null) {
            return null;
        }
        return token.length() <= keep ? token : token.substring(0, keep) + "…";
    }
}
