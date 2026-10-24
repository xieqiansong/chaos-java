package lan.chaos.oauth2.auth.token;

import lan.chaos.oauth2.auth.client.Client;
import lan.chaos.oauth2.auth.client.ClientRegistrationService;
import lan.chaos.oauth2.auth.authorize.AuthorizationCodeService;
import lan.chaos.oauth2.auth.authorize.CodeRecord;
import lan.chaos.oauth2.auth.common.constant.OAuth2Constant;
import lan.chaos.oauth2.auth.common.constant.OAuth2Endpoints;
import lan.chaos.oauth2.auth.common.model.Scope;
import lan.chaos.oauth2.auth.common.util.ClientAuthUtil;
import lan.chaos.oauth2.auth.common.util.OAuth2Errors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Token 端点（能力③）。支持两种 grant_type：
 * - authorization_code：用授权码换 access + refresh，并校验 PKCE verifier。
 * - refresh_token：滚动刷新（发新令牌，使旧 refresh 失效）。
 */
@RestController
public class TokenEndpoint {

    private final ClientRegistrationService clients;
    private final AuthorizationCodeService codeService;
    private final AccessTokenIssuer accessIssuer;
    private final RefreshTokenService refreshService;

    public TokenEndpoint(ClientRegistrationService clients,
                         AuthorizationCodeService codeService,
                         AccessTokenIssuer accessIssuer,
                         RefreshTokenService refreshService) {
        this.clients = clients;
        this.codeService = codeService;
        this.accessIssuer = accessIssuer;
        this.refreshService = refreshService;
    }

    @PostMapping(OAuth2Endpoints.TOKEN)
    public ResponseEntity<?> token(@RequestParam Map<String, String> params, HttpServletRequest request) {
        ClientAuthUtil.ClientCredentials creds = ClientAuthUtil.extract(request);
        Client client = clients.getClient(creds.getClientId()).orElse(null);
        if (client == null || !clients.authenticate(creds.getClientId(), creds.getClientSecret())) {
            return OAuth2Errors.error(401, "invalid_client", "客户端身份验证失败");
        }

        String grantType = params.get("grant_type");
        if (OAuth2Constant.GRANT_AUTHORIZATION_CODE.equals(grantType)) {
            return handleAuthorizationCode(client, params);
        } else if (OAuth2Constant.GRANT_REFRESH_TOKEN.equals(grantType)) {
            return handleRefreshToken(client, params);
        }
        return OAuth2Errors.error(400, "unsupported_grant_type", "不支持的 grant_type");
    }

    private ResponseEntity<?> handleAuthorizationCode(Client client, Map<String, String> params) {
        String code = params.get("code");
        String redirectUri = params.get("redirect_uri");
        String verifier = params.get("code_verifier");

        CodeRecord record;
        try {
            record = codeService.consume(code);
        } catch (IllegalStateException e) {
            return OAuth2Errors.error(400, "invalid_grant", e.getMessage());
        }
        if (!record.getClientId().equals(client.getClientId())) {
            return OAuth2Errors.error(400, "invalid_grant", "授权码不属于该客户端");
        }
        if (!equalsRedirect(record.getRedirectUri(), redirectUri)) {
            return OAuth2Errors.error(400, "invalid_grant", "redirect_uri 与授权时不一致");
        }
        // PKCE 校验（公共客户端强制；保密客户端若带 challenge 也校验）
        if (record.getCodeChallenge() != null) {
            if (verifier == null || !lan.chaos.oauth2.auth.common.util.PkceUtil.verify(verifier, record.getCodeChallenge())) {
                return OAuth2Errors.error(400, "invalid_grant", "PKCE code_verifier 校验失败");
            }
        }
        return issueTokens(client, record.getUserId(), record.getScopes(), null);
    }

    private ResponseEntity<?> handleRefreshToken(Client client, Map<String, String> params) {
        String refreshToken = params.get("refresh_token");
        RefreshRecord record = refreshService.get(refreshToken);
        if (record == null || record.isRevoked() || record.isExpired()) {
            return OAuth2Errors.error(400, "invalid_grant", "refresh_token 无效/已撤销/已过期");
        }
        if (!record.getClientId().equals(client.getClientId())) {
            return OAuth2Errors.error(400, "invalid_grant", "refresh_token 不属于该客户端");
        }
        // 滚动刷新：旧 refresh 立即失效，签发新令牌族（沿用 familyId）
        refreshService.revoke(refreshToken);
        return issueTokens(client, record.getUserId(), record.getScopes(), record.getFamilyId());
    }

    private ResponseEntity<?> issueTokens(Client client, String userId, Set<Scope> scopes, String familyId) {
        String accessToken = accessIssuer.issue(userId, client.getClientId(), scopes);
        String refreshToken = refreshService.issue(userId, client.getClientId(), scopes, familyId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", accessToken);
        body.put("token_type", "Bearer");
        body.put("expires_in", OAuth2Constant.ACCESS_TOKEN_TTL_SECONDS);
        body.put("refresh_token", refreshToken);
        body.put("scope", Scope.join(scopes));
        return ResponseEntity.ok(body);
    }

    private boolean equalsRedirect(String a, String b) {
        return a != null && a.equals(b);
    }
}
