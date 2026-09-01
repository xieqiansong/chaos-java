package lan.chaos.filterasync.config;

import lan.chaos.filterasync.service.ReportService;
import lan.chaos.filterasync.web.EarlyReportFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.concurrent.Executor;

@Configuration
public class FilterConfig {

    /**
     * 注册为最高优先级、拦截 /*，先于 DispatcherServlet 与所有拦截器执行。
     * 是否短路由 app.mode 决定（见 EarlyReportFilter）。
     */
    @Bean
    public FilterRegistrationBean<EarlyReportFilter> earlyReportFilter(
            ReportService reportService,
            @Qualifier("reportAsyncExecutor") Executor reportExecutor,
            @Value("${app.mode:filter-async}") String mode) {
        FilterRegistrationBean<EarlyReportFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new EarlyReportFilter(reportService, reportExecutor, mode));
        bean.addUrlPatterns("/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
