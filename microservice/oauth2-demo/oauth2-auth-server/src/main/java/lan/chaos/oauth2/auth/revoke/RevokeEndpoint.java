package lan.chaos.oauth2.auth.revoke;

import io.jsonwebtoken.JwtException;
import lan.chaos.oauth2.auth.client.ClientRegistrationService;
import lan.chaos.oauth2.auth.common.constant.OAuth2Endpoints;
import lan.chaos.oauth2.auth.common.util.ClientAuthUtil;
import lan.chaos.oauth2.auth.common.util.OAuth2Errors;
import lan.chaos.oauth2.auth.token.AccessTokenIssuer;
import lan.chaos.oauth2.auth.token.RefreshTokenService;
import lan.chaos.oauth2.auth.token.TokenStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 撤销端点（能力④）。RFC 7009：客户端主动撤销自身 Token。
 * - access_token：解析 JWT 取 jti，加入黑名单。
 * - refresh_token：标记 refresh 记录为已撤销（滚动刷新后旧 refresh 亦会命中此处）。
 * 返回 200（RFC 规定即使 token 未知也返回 200，避免泄露 token 是否存在）。
 */
@RestController
public class RevokeEndpoint {

    private final ClientRegistrationService clients;
    private final AccessTokenIssuer accessIssuer;
    private final RefreshTokenService refreshService;
    private final TokenStore tokenStore;

    public RevokeEndpoint(ClientRegistrationService clients, AccessTokenIssuer accessIssuer,
                          RefreshTokenService refreshService, TokenStore tokenStore) {
        this.clients = clients;
        this.accessIssuer = accessIssuer;
        this.refreshService = refreshService;
        this.tokenStore = tokenStore;
    }

    @PostMapping(OAuth2Endpoints.REVOKE)
    public ResponseEntity<Void> revoke(@RequestParam("token") String token,
                                       @RequestParam(value = "token_type_hint", required = false) String hint,
                                       HttpServletRequest request) {
        ClientAuthUtil.ClientCredentials creds = ClientAuthUtil.extract(request);
        if (!clients.authenticate(creds.getClientId(), creds.getClientSecret())) {
            return ResponseEntity.status(401).build();
        }

        // 先按 access_token 尝试（JWT）
        try {
            String jti = accessIssuer.parse(token).getId();
            tokenStore.revokeAccess(jti);
        } catch (JwtException ignored) {
            // 不是 JWT，可能是 refresh_token
        }
        // 再按 refresh_token 尝试
        if (refreshService.get(token) != null) {
            refreshService.revoke(token);
        }
        return ResponseEntity.ok().build();
    }
}
