package lan.chaos.microservice.common.security.config;

import lan.chaos.microservice.common.security.interceptor.PermissionInterceptor;
import lan.chaos.microservice.common.security.properties.JwtProperties;
import lan.chaos.microservice.common.security.util.JwtProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 自动配置：无论网关（WebFlux）还是下游服务（Servlet）都加载，
 * 提供全局唯一的 {@link JwtProvider} 与 {@link JwtProperties}。
 *
 * <p>只依赖 spring-context 级别，不碰 servlet/webmvc，因此网关也能安全引入。</p>
 * <p>{@link PermissionInterceptor} 的 Bean 放在这里（而非 SecurityWebConfig），用
 * {@code @ConditionalOnWebApplication(SERVLET)} 限定，避免「配置类构造器依赖自身 @Bean」的循环引用；
 * 网关是 WebFlux，不会创建它。</p>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public JwtProvider jwtProvider(JwtProperties props) {
        return new JwtProvider(props);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public PermissionInterceptor permissionInterceptor(JwtProvider jwtProvider) {
        return new PermissionInterceptor(jwtProvider);
    }
}
