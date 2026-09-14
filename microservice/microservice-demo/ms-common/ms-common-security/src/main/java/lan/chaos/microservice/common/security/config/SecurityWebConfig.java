package lan.chaos.microservice.common.security.config;

import lan.chaos.microservice.common.security.constant.SecurityConstants;
import lan.chaos.microservice.common.security.interceptor.PermissionInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把 {@link PermissionInterceptor} 注册进 Spring MVC 拦截器链，并对白名单路径放行。
 *
 * <p>@ConditionalOnWebApplication(type = SERVLET)：只在 Servlet 服务（user/order）生效，
 * <strong>不会</strong>在 WebFlux 网关加载——网关走自己的 {@code AuthGlobalFilter}。这样 common-security
 * 既能给网关供 JwtProvider，又能给下游服务供 MVC 拦截器，而两者互不影响。</p>
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityWebConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    /** 由 JwtAutoConfig 在同进程（Servlet）下产出的 PermissionInterceptor 注入，无循环依赖。 */
    public SecurityWebConfig(PermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(SecurityConstants.DEFAULT_WHITELIST);
    }
}
