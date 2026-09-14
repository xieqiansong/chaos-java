package lan.chaos.microservice.common.log.config;

import lan.chaos.microservice.common.log.aspect.AccessLogAspect;
import lan.chaos.microservice.common.log.filter.TraceIdFilter;
import lan.chaos.microservice.common.log.properties.AccessLogProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;

/**
 * 日志相关自动配置（被各服务 {@code @ComponentScan("lan.chaos.microservice")} 扫到后生效）。
 *
 * <p>职责：
 * <ol>
 *   <li>把 {@link TraceIdFilter} 注册成最高优先级的 Servlet 过滤器，确保进入业务前 MDC 已就位。</li>
 *   <li>P5：注册 {@link AccessLogAspect}（访问日志切面），默认开启，可用 {@code logging.access.enabled=false} 关闭。</li>
 * </ol>
 */
@Configuration
@EnableConfigurationProperties(AccessLogProperties.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class LogAutoConfig {

    @Value("${trace.header:X-Trace-Id}")
    private String traceHeader;

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> bean =
                new FilterRegistrationBean<>(new TraceIdFilter(traceHeader));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public AccessLogAspect accessLogAspect(AccessLogProperties properties) {
        return new AccessLogAspect(properties);
    }
}
