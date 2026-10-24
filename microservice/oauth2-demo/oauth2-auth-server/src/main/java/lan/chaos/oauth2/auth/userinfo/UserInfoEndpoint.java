package lan.chaos.oauth2.auth.userinfo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lan.chaos.oauth2.auth.common.constant.OAuth2Constant;
import lan.chaos.oauth2.auth.common.constant.OAuth2Endpoints;
import lan.chaos.oauth2.auth.common.model.SampleUser;
import lan.chaos.oauth2.auth.common.model.Scope;
import lan.chaos.oauth2.auth.common.model.User;
import lan.chaos.oauth2.auth.common.util.OAuth2Errors;
import lan.chaos.oauth2.auth.token.AccessTokenIssuer;
import lan.chaos.oauth2.auth.token.TokenStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 授权服务器侧 userInfo 端点。受 Bearer access_token 保护：
 * 验签 → 查黑名单 → 查过期 → 校验 scope，再返回用户信息（OIDC 风格）。
 */
@RestController
public class UserInfoEndpoint {

    private final AccessTokenIssuer accessIssuer;
    private final TokenStore tokenStore;

    public UserInfoEndpoint(AccessTokenIssuer accessIssuer, TokenStore tokenStore) {
        this.accessIssuer = accessIssuer;
        this.tokenStore = tokenStore;
    }

    @GetMapping(OAuth2Endpoints.USERINFO)
    public ResponseEntity<?> userinfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return OAuth2Errors.error(401, "invalid_token", "缺少 Bearer access_token");
        }
        String token = authorization.substring(7);
        Claims claims;
        try {
            claims = accessIssuer.parse(token);
        } catch (JwtException e) {
            return OAuth2Errors.error(401, "invalid_token", "Token 签名/格式无效");
        }
        if (tokenStore.isAccessRevoked(claims.getId())) {
            return OAuth2Errors.error(401, "invalid_token", "Token 已被撤销");
        }
        if (claims.getExpiration().before(new java.util.Date())) {
            return OAuth2Errors.error(401, "invalid_token", "Token 已过期");
        }
        String scopeStr = claims.get("scope", String.class);
        if (!Scope.parse(scopeStr).contains(Scope.USER_BASIC)) {
            return OAuth2Errors.error(403, "insufficient_scope", "缺少 user.basic 权限");
        }
        User user = SampleUser.sampleUser(claims.getSubject());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", user.getUsername());
        body.put("name", user.getName());
        body.put("email", user.getEmail());
        body.put("scope", scopeStr);
        return ResponseEntity.ok(body);
    }
}
