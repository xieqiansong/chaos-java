package lan.chaos.microservice.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.common.security.properties.JwtProperties;
import lan.chaos.microservice.common.security.util.JwtProvider;
import lan.chaos.microservice.common.security.config.JwtAutoConfig;
import lan.chaos.microservice.common.security.config.SecurityWebConfig;
import lan.chaos.microservice.common.web.advice.GlobalExceptionHandler;
import lan.chaos.microservice.common.web.advice.ResponseAdvice;
import lan.chaos.microservice.user.entity.User;
import lan.chaos.microservice.user.mapper.AccountMapper;
import lan.chaos.microservice.user.mapper.UserMapper;
import lan.chaos.microservice.user.mapper.UserTagMapper;
import lan.chaos.microservice.user.model.CreateUserRequest;
import lan.chaos.microservice.user.service.AccountService;
import lan.chaos.microservice.user.service.UserService;
import lan.chaos.microservice.user.service.UserTagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web 层切片测试（@WebMvcTest）：只加载 UserController + 两个横切 Advice，
 * service/mapper 全部用 @MockBean 桩掉，不连数据库、不连 Nacos，离线即可跑。
 *
 * <p>注意：@WebMvcTest 仍会经由 @SpringBootApplication 的 @MapperScan 注册出 Mapper Bean，
 * 切片环境没有 DataSource，Mapper 会因缺 sqlSessionFactory 初始化失败；因此把 Mapper 也 @MockBean 掉，
 * 让它们以 mock 形态注册，绕开数据层。这正验证了“Web 切片测试不该触碰数据层”的原则。</p>
 */
@WebMvcTest(controllers = UserController.class)
@Import({GlobalExceptionHandler.class, ResponseAdvice.class, JwtAutoConfig.class, SecurityWebConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserTagService userTagService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private UserTagMapper userTagMapper;

    @MockBean
    private AccountMapper accountMapper;

    // P4：PermissionInterceptor 需要 JwtProvider；切片环境用 mock 提供，并委托给真实实例校验我们签发的 token
    @MockBean
    private JwtProvider jwtProvider;

    private JwtProvider realProvider() {
        JwtProperties props = new JwtProperties();
        props.setSecret("chaos-demo-secret-key-please-replace-32-bytes");
        return new JwtProvider(props);
    }

    private void stubJwtDelegateTo(JwtProvider real) {
        when(jwtProvider.validateToken(anyString())).thenAnswer(i -> real.validateToken(i.getArgument(0)));
        when(jwtProvider.isAccessToken(anyString())).thenAnswer(i -> real.isAccessToken(i.getArgument(0)));
        when(jwtProvider.getLoginUser(anyString())).thenAnswer(i -> real.getLoginUser(i.getArgument(0)));
    }

    private String tokenFor(String username, Long userId, String... perms) {
        JwtProvider real = realProvider();
        LoginUser u = new LoginUser(userId, username, new HashSet<>(Arrays.asList(perms)));
        return "Bearer " + real.generateAccessToken(u);
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void me_withAdminToken_returnsCurrentUser() throws Exception {
        stubJwtDelegateTo(realProvider());
        mockMvc.perform(get("/users/me").header("Authorization", tokenFor("admin", 1L, "user:read", "user:write")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    void delete_withGuestToken_returns403() throws Exception {
        stubJwtDelegateTo(realProvider());
        // guest 只有 user:read，访问需 user:write 的删除接口 → 403
        mockMvc.perform(delete("/users/1").header("Authorization", tokenFor("guest", 2L, "user:read")))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void delete_withAdminToken_returns200() throws Exception {
        stubJwtDelegateTo(realProvider());
        mockMvc.perform(delete("/users/1").header("Authorization", tokenFor("admin", 1L, "user:read", "user:write")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void createUser_shouldWrapDomainIntoR() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setAge(20);
        user.setPhone("123");
        given(userService.createUser(any(), any(), any(), any())).willReturn(user);

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("alice");
        req.setNickname("Alice");
        req.setAge(20);
        req.setPhone("123");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void getUser_notFound_shouldReturnFailWith404() throws Exception {
        given(userService.getUser(eq(99L))).willThrow(new BizException(ResultCode.NOT_FOUND));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(ResultCode.NOT_FOUND.getMessage()));
    }
}
