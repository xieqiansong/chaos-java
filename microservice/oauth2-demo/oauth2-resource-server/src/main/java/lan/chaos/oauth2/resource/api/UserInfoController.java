package lan.chaos.oauth2.resource.api;

import lan.chaos.oauth2.resource.common.constant.ResourceConstant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 受保护的业务资源。只有携带合法 Bearer access_token（且 scope 含 user.basic）才能访问。
 * 验签由 Spring Security 的 OAuth2 资源服务器完成（JWK 公钥来自授权服务器）。
 */
@RestController
public class UserInfoController {

    @GetMapping(ResourceConstant.USER_INFO_ENDPOINT)
    public Map<String, Object> userInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sub", jwt.getSubject());
        body.put("scope", jwt.getClaimAsString("scope"));
        body.put("client_id", jwt.getClaimAsString("client_id"));
        body.put("message", "资源服务器已用授权服务器公钥验签通过");
        return body;
    }
}
