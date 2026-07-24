package lan.chaos.microservice.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 用户服务启动类。
 *
 * <p>@ComponentScan("lan.chaos.microservice") 是为了把 common 层（common-log 的 TraceId 过滤器、
 * common-web 的全局异常/响应包装）的 @Configuration / @RestControllerAdvice 也扫进容器——
 * 它们不在本服务自己的包路径下，默认扫不到。</p>
 *
 * <p>Mapper 扫描在 {@code config/MybatisPlusConfig} 上，避免与启动类重复扫描导致 Bean 重复注册。</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("lan.chaos.microservice")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
