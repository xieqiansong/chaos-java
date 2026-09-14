package lan.chaos.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WebFlux 响应式编程 Demo 启动类。
 * 仅引入 spring-boot-starter-webflux（无 spring-boot-starter-web），底层跑 Netty 非阻塞服务器。
 */
@SpringBootApplication
public class WebFluxApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebFluxApplication.class, args);
    }
}
