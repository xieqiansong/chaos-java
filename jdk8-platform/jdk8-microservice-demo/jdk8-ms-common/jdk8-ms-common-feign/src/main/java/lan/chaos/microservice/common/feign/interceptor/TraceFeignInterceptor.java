package lan.chaos.microservice.common.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lan.chaos.microservice.common.core.constant.TraceConstants;
import lan.chaos.microservice.common.security.constant.SecurityConstants;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * ★★★ P1 链路透传拦截器（Feign）：把「上游请求」的链路与身份原样带下去，保证跨进程一条 traceId。
 *
 * <p>WHY：在微服务里一个请求会穿过网关 → user → order 等多个进程，如果不把 traceId 往下透传，
 * 各进程的日志各记各的，线上排查问题时根本串不起来。网关在入口生成 X-Trace-Id，这里负责把它
 * 通过 Header 继续往下传，下游再用 {@code TraceIdFilter} 接回 MDC。</p>
 *
 * <p>关键点：
 * <ol>
 *   <li>traceId 优先取上游请求头，其次降级取本进程 MDC（比如由异步线程触发 Feign 调用时）。</li>
 *   <li>始终附带 {@code Authorization}，让下游服务能继续做鉴权（网关只拦未认证，鉴权逻辑在下游）。</li>
 *   <li>P5 增强：附带调用方身份头 {@code X-User-Id}/{@code X-User-Name}（来自 MDC，由下游拦截器写入），
 *       仅作上下文 / 日志用途，下游鉴权仍以 JWT 为准，不靠这两个头放行。</li>
 * </ol>
 */
public class TraceFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 1) 透传 traceId：优先取上游请求头，其次取本进程 MDC
        String traceId = resolveInheritedTraceId();
        if (traceId == null || traceId.isEmpty()) {
            traceId = MDC.get(TraceConstants.TRACE_ID_MDC_KEY);
        }
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TraceConstants.TRACE_ID_HEADER, traceId);
        }

        // 2) 透传 Authorization：让下游服务能继续做鉴权
        String auth = resolveHeaderFromRequest(SecurityConstants.AUTHORIZATION_HEADER);
        if (auth != null && !auth.isEmpty()) {
            template.header(SecurityConstants.AUTHORIZATION_HEADER, auth);

            // 3) P5 增强：透传调用方身份（上下文用，非鉴权依据），取自 MDC（下游拦截器写入）
            putIfPresent(template, TraceConstants.USER_ID_HEADER, MDC.get(TraceConstants.USER_ID_MDC_KEY));
            putIfPresent(template, TraceConstants.USER_NAME_HEADER, MDC.get(TraceConstants.USER_NAME_MDC_KEY));
        }
    }

    private void putIfPresent(RequestTemplate template, String header, String value) {
        if (value != null && !value.isEmpty()) {
            template.header(header, value);
        }
    }

    private String resolveInheritedTraceId() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader(TraceConstants.TRACE_ID_HEADER);
    }

    private String resolveHeaderFromRequest(String headerName) {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader(headerName);
    }

    private HttpServletRequest currentRequest() {
        Object attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).getRequest();
        }
        return null;
    }
}
