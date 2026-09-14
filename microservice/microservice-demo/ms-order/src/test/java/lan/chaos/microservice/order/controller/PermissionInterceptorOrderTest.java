package lan.chaos.microservice.order.controller;

import lan.chaos.microservice.common.security.interceptor.PermissionInterceptor;
import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.common.security.properties.JwtProperties;
import lan.chaos.microservice.common.security.util.JwtProvider;
import lan.chaos.microservice.order.model.Order;
import lan.chaos.microservice.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 订单服务方法级权限测试（纯单元，不加载 Spring 上下文，绕开 Feign/Nacos 初始化）。
 *
 * <p>直接把 {@link PermissionInterceptor} 套到 OrderController 的真实方法（带 @RequiresPermission）上，
 * 用 MockHttpServletRequest 伪造不同角色 token，断言「无 token→401 / 权限不足→403 / 权限足够→放行」。</p>
 */
class PermissionInterceptorOrderTest {

    private JwtProvider jwtProvider;

    private OrderController controller;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("chaos-demo-secret-key-please-replace-32-bytes");
        jwtProvider = new JwtProvider(props);

        OrderService orderService = mock(OrderService.class);
        when(orderService.create(1L, null)).thenReturn(new Order());
        controller = new OrderController(orderService);
    }

    private String token(String username, Long userId, String... perms) {
        LoginUser u = new LoginUser(userId, username, new HashSet<>(Arrays.asList(perms)));
        return "Bearer " + jwtProvider.generateAccessToken(u);
    }

    private MockHttpServletResponse applyTo(String methodName, String token, Class<?>... paramTypes) throws Exception {
        PermissionInterceptor interceptor = new PermissionInterceptor(jwtProvider);
        Method method = OrderController.class.getMethod(methodName, paramTypes);
        HandlerMethod hm = new HandlerMethod(controller, method);
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (token != null) {
            req.addHeader("Authorization", token);
        }
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean pass = interceptor.preHandle(req, resp, hm);
        // 返回结果在 resp 里（401/403）或 pass 标志里（放行）
        if (token == null) {
            assertFalse(pass);
        }
        return resp;
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        MockHttpServletResponse resp = applyTo("create", null, Long.class, java.math.BigDecimal.class);
        assertEquals(401, resp.getStatus());
    }

    @Test
    void create_withGuest_forbidden() throws Exception {
        MockHttpServletResponse resp = applyTo("create", token("guest", 2L, "user:read"),
                Long.class, java.math.BigDecimal.class);
        assertEquals(403, resp.getStatus());
    }

    @Test
    void create_withAdmin_passes() throws Exception {
        MockHttpServletResponse resp = applyTo("create", token("admin", 1L, "order:write", "user:read"),
                Long.class, java.math.BigDecimal.class);
        // 放行：拦截器返回 true，响应状态保持默认 200
        assertTrue(resp.getStatus() == 200);
    }

    @Test
    void me_withoutOrderRead_forbidden() throws Exception {
        // guest 只有 user:read，没有 order:read → /orders/me 返回 403
        MockHttpServletResponse resp = applyTo("me", token("guest", 2L, "user:read"));
        assertEquals(403, resp.getStatus());
    }

    @Test
    void me_withAdmin_passes() throws Exception {
        MockHttpServletResponse resp = applyTo("me", token("admin", 1L, "order:read"));
        assertTrue(resp.getStatus() == 200);
    }
}
