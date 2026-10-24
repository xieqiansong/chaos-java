package lan.chaos.oauth2.auth.introspect;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lan.chaos.oauth2.auth.client.ClientRegistrationService;
import lan.chaos.oauth2.auth.common.constant.OAuth2Endpoints;
import lan.chaos.oauth2.auth.common.model.Scope;
import lan.chaos.oauth2.auth.common.util.ClientAuthUtil;
import lan.chaos.oauth2.auth.common.util.OAuth2Errors;
import lan.chaos.oauth2.auth.token.AccessTokenIssuer;
import lan.chaos.oauth2.auth.token.RefreshTokenService;
import lan.chaos.oauth2.auth.token.RefreshRecord;
import lan.chaos.oauth2.auth.token.TokenStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自省端点（能力⑤）。RFC 7662：供资源服务器/调试查询 Token 状态。
 * 返回 {active:true/false, ...claims}；被撤销（黑名单）或过期都返回 active:false。
 */
@RestController
public class IntrospectEndpoint {

    private final ClientRegistrationService clients;
    private final AccessTokenIssuer accessIssuer;
    private final RefreshTokenService refreshService;
    private final TokenStore tokenStore;

    public IntrospectEndpoint(ClientRegistrationService clients, AccessTokenIssuer accessIssuer,
                              RefreshTokenService refreshService, TokenStore tokenStore) {
        this.clients = clients;
        this.accessIssuer = accessIssuer;
        this.refreshService = refreshService;
        this.tokenStore = tokenStore;
    }

    @PostMapping(OAuth2Endpoints.INTROSPECT)
    public ResponseEntity<?> introspect(@RequestParam("token") String token,
                                        @RequestParam(value = "token_type_hint", required = false) String hint,
                                        HttpServletRequest request) {
        ClientAuthUtil.ClientCredentials creds = ClientAuthUtil.extract(request);
        if (!clients.authenticate(creds.getClientId(), creds.getClientSecret())) {
            return OAuth2Errors.error(401, "invalid_client", "客户端身份验证失败");
        }

        // 先尝试作为 access_token（JWT）
        try {
            Claims claims = accessIssuer.parse(token);
            boolean revoked = tokenStore.isAccessRevoked(claims.getId());
            boolean expired = claims.getExpiration().before(new java.util.Date());
            if (!revoked && !expired) {
                return ResponseEntity.ok(activeAccess(claims));
            }
            return ResponseEntity.ok(inactive());
        } catch (JwtException ignored) {
            // 可能是 refresh_token
        }

        RefreshRecord rr = refreshService.get(token);
        if (rr != null && !rr.isRevoked() && !rr.isExpired()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("active", true);
            body.put("token_type", "refresh_token");
            body.put("client_id", rr.getClientId());
            body.put("username", rr.getUserId());
            body.put("scope", Scope.join(rr.getScopes()));
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.ok(inactive());
    }

    private Map<String, Object> activeAccess(Claims claims) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("active", true);
        body.put("token_type", "access_token");
        body.put("client_id", claims.get("client_id"));
        body.put("username", claims.getSubject());
        body.put("scope", claims.get("scope"));
        body.put("exp", claims.getExpiration().getTime() / 1000L);
        return body;
    }

    private Map<String, Object> inactive() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("active", false);
        return body;
    }
}
