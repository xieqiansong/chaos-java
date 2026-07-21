package lan.chaos.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证授权 Demo 启动类。启动后可用 curl 把玩四个端点：
 *
 * <pre>
 * curl localhost:8080/api/public
 * curl -u alice:secret localhost:8080/api/secure
 * curl -X POST localhost:8080/api/token?user=alice
 * curl -H "Authorization: Bearer &lt;上一步拿到的token&gt;" localhost:8080/api/jwt-secure
 * </pre>
 */
@SpringBootApplication
public class SecurityApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }
}
