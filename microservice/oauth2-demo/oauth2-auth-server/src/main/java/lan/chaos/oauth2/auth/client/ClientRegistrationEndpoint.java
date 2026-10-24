package lan.chaos.oauth2.auth.client;

import lan.chaos.oauth2.auth.client.ClientRegistrationService;
import lan.chaos.oauth2.auth.common.constant.OAuth2Endpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端注册端点（演示用）。生产应对此端点做管理员鉴权与 redirect_uri 安全校验。
 */
@RestController
public class ClientRegistrationEndpoint {

    private final ClientRegistrationService service;

    public ClientRegistrationEndpoint(ClientRegistrationService service) {
        this.service = service;
    }

    @PostMapping(OAuth2Endpoints.CLIENTS)
    public ResponseEntity<ClientRegistrationResult> register(@RequestBody ClientRegistrationRequest request) {
        return ResponseEntity.ok(service.register(request));
    }
}
