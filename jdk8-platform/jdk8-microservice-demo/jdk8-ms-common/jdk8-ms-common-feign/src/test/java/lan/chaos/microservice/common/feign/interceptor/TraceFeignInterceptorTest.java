package lan.chaos.microservice.common.feign.interceptor;

import feign.RequestTemplate;
import lan.chaos.microservice.common.core.constant.TraceConstants;
import lan.chaos.microservice.common.security.constant.SecurityConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TraceFeignInterceptor 单测：验证跨进程透传的链路与身份头。
 * 不依赖任何中间件，直接构造 Servlet 请求上下文 + MDC 即可断言。
 */
class TraceFeignInterceptorTest {

    private final TraceFeignInterceptor interceptor = new TraceFeignInterceptor();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void shouldPropagateTraceIdAndAuthorizationAndIdentityHeaders() {
        // 模拟一个被上游 HTTP 请求触发的 Feign 调用：请求头带了 traceId / Authorization
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(TraceConstants.TRACE_ID_HEADER)).thenReturn("trace-abc");
        when(req.getHeader(SecurityConstants.AUTHORIZATION_HEADER)).thenReturn("Bearer jwt-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        // 下游拦截器已把登录身份写进 MDC
        MDC.put(TraceConstants.USER_ID_MDC_KEY, "1001");
        MDC.put(TraceConstants.USER_NAME_MDC_KEY, "admin");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertEquals("trace-abc", first(template, TraceConstants.TRACE_ID_HEADER));
        assertEquals("Bearer jwt-token", first(template, SecurityConstants.AUTHORIZATION_HEADER));
        // P5 增强：调用方身份透传给下游（仅上下文用）
        assertEquals("1001", first(template, TraceConstants.USER_ID_HEADER));
        assertEquals("admin", first(template, TraceConstants.USER_NAME_HEADER));
    }

    @Test
    void shouldFallbackToMdcTraceIdWhenNoRequest() {
        // 非 HTTP 线程（如异步/定时任务）触发 Feign：没有 ServletRequestAttributes，仅 MDC 有 traceId
        MDC.put(TraceConstants.TRACE_ID_MDC_KEY, "trace-mdc");

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertEquals("trace-mdc", first(template, TraceConstants.TRACE_ID_HEADER));
        // 没有 Authorization，不应带身份头
        assertFalse(template.headers().containsKey(TraceConstants.USER_ID_HEADER));
        assertFalse(template.headers().containsKey(TraceConstants.USER_NAME_HEADER));
    }

    @Test
    void shouldNotAddIdentityWhenNoAuthorization() {
        // 内部未登录链路：有 traceId 但无 Authorization，身份头不强行加
        RequestTemplate template = new RequestTemplate();
        template.header(TraceConstants.TRACE_ID_HEADER, "trace-x");
        MDC.put(TraceConstants.USER_ID_MDC_KEY, "1");
        interceptor.apply(template);

        assertTrue(template.headers().containsKey(TraceConstants.TRACE_ID_HEADER));
        assertFalse(template.headers().containsKey(TraceConstants.USER_ID_HEADER));
    }

    private String first(RequestTemplate template, String header) {
        java.util.Collection<String> values = template.headers().get(header);
        return values == null || values.isEmpty() ? null : values.iterator().next();
    }
}
