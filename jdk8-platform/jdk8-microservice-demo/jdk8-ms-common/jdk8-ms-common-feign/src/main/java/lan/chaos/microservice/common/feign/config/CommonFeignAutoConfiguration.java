package lan.chaos.microservice.common.feign.config;

import lan.chaos.microservice.common.feign.interceptor.TraceFeignInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 公共自动配置（被各服务 {@code @ComponentScan("lan.chaos.microservice")} 扫到后生效）。
 *
 * <p>只负责注册透传拦截器；Feign Client 的定义与 {@code @EnableFeignClients} 由各服务自己声明。
 * 熔断降级能力由各服务在 yml 中开启 {@code feign.sentinel.enabled=true} 后由 Sentinel 接管。</p>
 */
@Configuration
public class CommonFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraceFeignInterceptor traceFeignInterceptor() {
        return new TraceFeignInterceptor();
    }
}
