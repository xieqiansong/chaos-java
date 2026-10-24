package lan.chaos.oauth2.auth.authorize;

import lan.chaos.oauth2.auth.client.Client;
import lan.chaos.oauth2.auth.client.ClientRegistrationService;
import lan.chaos.oauth2.auth.common.constant.OAuth2Endpoints;
import lan.chaos.oauth2.auth.common.model.Scope;
import lan.chaos.oauth2.auth.common.util.OAuth2Errors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 授权端点（能力②）。纯 API 形态：用户用 HTTP Basic 登录（资源拥有者认证），
 * 校验客户端/redirect_uri/scope/PKCE 后，要么直接发授权码（auto_consent），要么返回 consent_id 等待批准。
 *
 * <p>生产应强制 HTTPS；redirect_uri 必须严格等于注册值防钓鱼；state 原样回传防 CSRF。</p>
 */
@RestController
public class AuthorizeEndpoint {

    private final ClientRegistrationService clients;
    private final AuthorizationCodeService codeService;
    private final ConsentService consentService;

    public AuthorizeEndpoint(ClientRegistrationService clients,
                             AuthorizationCodeService codeService,
                             ConsentService consentService) {
        this.clients = clients;
        this.codeService = codeService;
        this.consentService = consentService;
    }

    @GetMapping(OAuth2Endpoints.AUTHORIZE)
    public ResponseEntity<?> authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            @RequestParam(value = "response_mode", defaultValue = "json") String responseMode,
            @RequestParam(value = "auto_consent", defaultValue = "false") boolean autoConsent,
            Authentication authentication) {

        String userId = authentication.getName();

        Client client = clients.getClient(clientId).orElse(null);
        if (client == null) {
            return OAuth2Errors.error(401, "invalid_client", "unknown client_id");
        }
        if (!client.getRedirectUris().contains(redirectUri)) {
            return OAuth2Errors.error(400, "invalid_request", "redirect_uri 与注册值不一致（防钓鱼）");
        }
        Set<Scope> requested = Scope.parse(scope);
        if (!Scope.isSubset(requested, client.getScopes())) {
            return OAuth2Errors.error(400, "invalid_scope", "申请的 scope 超出客户端被授权范围（权限最小化）");
        }
        if (client.isPkceRequired()) {
            if (codeChallenge == null || !"S256".equals(codeChallengeMethod)) {
                return OAuth2Errors.error(400, "invalid_request", "公共客户端必须携带 code_challenge(S256)");
            }
        }

        if (autoConsent) {
            String code = codeService.issue(clientId, userId, requested, redirectUri, codeChallenge);
            if ("json".equalsIgnoreCase(responseMode)) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("code", code);
                body.put("state", state);
                return ResponseEntity.ok(body);
            }
            String sep = redirectUri.contains("?") ? "&" : "?";
            return ResponseEntity.status(302)
                    .location(java.net.URI.create(redirectUri + sep + "code=" + code
                            + (state != null ? "&state=" + state : "")))
                    .build();
        }

        // 非自动确认：返回 consent_id 与 scope 文案，等待 POST /oauth2/consent 决定
        ConsentRecord consent = consentService.create(clientId, userId, requested, redirectUri, state, codeChallenge);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("consent_id", consent.getConsentId());
        body.put("client_name", client.getClientName());
        body.put("scopes", consentService.describeScopes(requested));
        body.put("state", state);
        return ResponseEntity.ok(body);
    }
}
