package lan.chaos.oauth2.auth.common.util;

import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/** 统一返回 OAuth2 错误响应 {error, error_description}。 */
public final class OAuth2Errors {

    private OAuth2Errors() {
    }

    public static ResponseEntity<Map<String, Object>> error(int status, String code, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("error_description", description);
        return ResponseEntity.status(status).body(body);
    }
}
