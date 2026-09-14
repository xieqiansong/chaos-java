package lan.chaos.microservice.common.security.interceptor;

import lan.chaos.microservice.common.security.annotation.Logical;
import lan.chaos.microservice.common.security.annotation.RequiresPermission;
import lan.chaos.microservice.common.security.constant.SecurityConstants;
import lan.chaos.microservice.common.security.context.LoginUserContext;
import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.common.security.util.JwtProvider;
import lan.chaos.microservice.common.core.constant.TraceConstants;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * ★★★ P4 细粒度授权拦截器（Servlet 环境，@ConditionalOnWebApplication(SERVLET) 保证只在下游服务加载）。
 *
 * <p>职责（请求进入 Controller 前）：
 * <ol>
 *   <li>从 {@code Authorization: Bearer xxx} 取 token，用 {@link JwtProvider} 校验并还原 {@link LoginUser}，
 *       放进 {@link LoginUserContext}（Controller/Service 用 {@code LoginUserContext.get()} 取当前用户）。</li>
 *   <li>若目标方法打了 {@link RequiresPermission}，再校验当前用户是否具备所需权限；否则 401/403 直接响应。</li>
 * </ol>
 *
 * <p>注意：粗粒度「是否登录」已由网关把关，这里默认放行公开接口（无注解），只对有注解的方法强制鉴权。
 * 这样公开接口无需 token 也能访问，而受保护接口即使绕过网关也会被这里拦下。</p>
 */
public class PermissionInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    public PermissionInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        LoginUser user = resolveUser(request);
        // 公开接口（无注解）即使无 token 也放行；有 token 则把身份放进上下文供业务取用
        LoginUserContext.set(user);
        // 把登录身份写进 MDC：既让本进程的访问日志能带上「谁在操作」，也让 Feign 拦截器能透传给下游
        if (user != null) {
            MDC.put(TraceConstants.USER_ID_MDC_KEY, String.valueOf(user.getUserId()));
            MDC.put(TraceConstants.USER_NAME_MDC_KEY, user.getUsername());
        }

        if (handler instanceof HandlerMethod) {
            RequiresPermission rp = ((HandlerMethod) handler).getMethodAnnotation(RequiresPermission.class);
            if (rp != null) {
                if (user == null) {
                    writeJson(response, 401, "未认证或 token 无效，请先登录");
                    return false;
                }
                boolean ok = rp.logical() == Logical.OR
                        ? Arrays.stream(rp.value()).anyMatch(user::hasPermission)
                        : Arrays.stream(rp.value()).allMatch(user::hasPermission);
                if (!ok) {
                    writeJson(response, 403, "权限不足，需要其中一项：" + Arrays.toString(rp.value()));
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束必须清理，避免线程池复用把上一个用户的身份/MDC 串到下一个请求
        LoginUserContext.clear();
        MDC.remove(TraceConstants.USER_ID_MDC_KEY);
        MDC.remove(TraceConstants.USER_NAME_MDC_KEY);
    }

    private LoginUser resolveUser(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return null;
        }
        String token = header.substring(SecurityConstants.TOKEN_PREFIX.length());
        if (!jwtProvider.validateToken(token)) {
            return null;
        }
        return jwtProvider.getLoginUser(token);
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        // 手动拼 R 的标准形状 {code,message,data,timestamp}，避免本模块引入 Jackson 依赖
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
                code, escape(message), System.currentTimeMillis());
        response.getWriter().write(body);
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\\' || c == '"') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
