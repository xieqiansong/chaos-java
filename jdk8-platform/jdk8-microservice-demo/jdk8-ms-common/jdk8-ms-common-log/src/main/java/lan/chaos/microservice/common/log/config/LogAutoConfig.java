package lan.chaos.microservice.common.log.config;

import lan.chaos.microservice.common.log.filter.TraceIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 日志相关自动配置（被各服务 {@code @ComponentScan("lan.chaos.microservice")} 扫到后生效）。
 *
 * <p>把 {@link TraceIdFilter} 注册成最高优先级的 Servlet 过滤器，确保进入业务前 MDC 已就位。</p>
 */
@Configuration
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
}
