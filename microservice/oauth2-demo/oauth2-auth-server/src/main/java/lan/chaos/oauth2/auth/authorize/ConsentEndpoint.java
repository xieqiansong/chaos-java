package lan.chaos.oauth2.auth.authorize;

import lan.chaos.oauth2.auth.common.constant.OAuth2Endpoints;
import lan.chaos.oauth2.auth.common.util.OAuth2Errors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 授权确认端点（能力②）。用户在授权确认页点「批准/拒绝」后调用的 API 形态。
 * 批准后签发授权码（纯 API：直接返回 {code, state}，不依赖浏览器重定向）。
 */
@RestController
public class ConsentEndpoint {

    private final ConsentService consentService;

    public ConsentEndpoint(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping(OAuth2Endpoints.CONSENT)
    public ResponseEntity<?> consent(
            @RequestParam("consent_id") String consentId,
            @RequestParam("decision") String decision,
            Authentication authentication) {

        ConsentRecord record = consentService.get(consentId);
        if (record == null) {
            return OAuth2Errors.error(400, "invalid_request", "consent not found");
        }
        // 防越权：只能确认自己的授权请求
        if (!record.getUserId().equals(authentication.getName())) {
            return OAuth2Errors.error(403, "access_denied", "consent 不属于当前用户");
        }

        if ("approve".equalsIgnoreCase(decision)) {
            String code = consentService.approve(consentId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", code);
            body.put("state", record.getState());
            return ResponseEntity.ok(body);
        } else if ("deny".equalsIgnoreCase(decision)) {
            consentService.deny(consentId);
            return OAuth2Errors.error(403, "access_denied", "用户拒绝授权");
        }
        return OAuth2Errors.error(400, "invalid_request", "decision 必须为 approve/deny");
    }
}
