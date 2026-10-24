package lan.chaos.oauth2.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OAuth2 资源服务器启动类。仅做一件事：用授权服务器的 JWK 公钥验签 Bearer JWT，保护业务接口。
 * 这正是 security-demo 里那个「外部 IdP」的对应角色——本仓库的 auth-server 即该 IdP。
 */
@SpringBootApplication
public class ResourceServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceServerApplication.class, args);
    }
}
