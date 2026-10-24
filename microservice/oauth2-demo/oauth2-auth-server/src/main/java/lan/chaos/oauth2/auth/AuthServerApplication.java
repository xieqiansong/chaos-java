package lan.chaos.oauth2.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OAuth2 授权服务器启动类（IdP）。
 * 根包 lan.chaos.oauth2.auth；模块内按能力分包（client/authorize/token/revoke/introspect/jwks/userinfo）。
 */
@SpringBootApplication
public class AuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}
