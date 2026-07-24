package lan.chaos.microservice.common.feign.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lan.chaos.microservice.common.core.constant.TraceConstants;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Feign 请求拦截器：把入口的 traceId 与 Authorization 透传给下游服务。
 *
 * <p>WHY：网关/上游生成的 {@code X-Trace-Id} 必须跨进程连续，否则链路日志断片、无法串联；
 * Authorization 透传让下游无需再校验一次（网关已统一鉴权，P4 落地）。</p>
 *
 * <p>注册为 Bean 后即被所有 Feign Client 自动套用（OpenFeign 收集容器内的 {@link RequestInterceptor}）。</p>
 */
public class TraceFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 1) 透传 traceId（由 common-log 的 TraceIdFilter 在入口写入 MDC）
        String traceId = MDC.get(TraceConstants.TRACE_ID_MDC_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TraceConstants.TRACE_ID_HEADER, traceId);
        }
        // 2) 透传 Authorization（当前请求线程里若有 ServletRequestAttributes，说明是被 HTTP 请求触发的调用）
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String auth = request.getHeader("Authorization");
            if (auth != null && !auth.isEmpty()) {
                template.header("Authorization", auth);
            }
        }
    }
}
