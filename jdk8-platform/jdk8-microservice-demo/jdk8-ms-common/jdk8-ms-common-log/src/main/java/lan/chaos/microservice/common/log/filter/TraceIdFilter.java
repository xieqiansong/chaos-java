package lan.chaos.microservice.common.log.filter;

import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * 全链路 traceId 过滤器（Servlet 环境）。
 *
 * <p>WHY：分布式系统排障最关键的是“一次请求跨多个服务”能串起来。约定由【网关】在入口生成
 * traceId 并通过请求头（默认 {@code X-Trace-Id}）往下传；本过滤器在不带 traceId 时兜底自生成，
 * 并把 traceId 放进 SLF4J 的 {@link MDC}，logback 的 pattern 里用 {@code %X{traceId}} 即可打印，
 * 下游服务/Feign 透传时只需读同一请求头续接即可，整条链路日志同 id。</p>
 *
 * <p>生产坑：必须在 {@code finally} 里 {@code MDC.remove}，否则线程池复用会串号（经典内存泄漏/串日志）。</p>
 */
public class TraceIdFilter implements Filter {

    private final String traceHeader;

    public TraceIdFilter(String traceHeader) {
        this.traceHeader = traceHeader;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String traceId = httpRequest.getHeader(traceHeader);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put("traceId", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
