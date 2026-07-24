package lan.chaos.microservice.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lan.chaos.microservice.common.core.exception.BizException;
import lan.chaos.microservice.common.core.result.ResultCode;
import lan.chaos.microservice.common.web.advice.GlobalExceptionHandler;
import lan.chaos.microservice.common.web.advice.ResponseAdvice;
import lan.chaos.microservice.user.entity.User;
import lan.chaos.microservice.user.mapper.UserMapper;
import lan.chaos.microservice.user.mapper.UserTagMapper;
import lan.chaos.microservice.user.model.CreateUserRequest;
import lan.chaos.microservice.user.service.UserService;
import lan.chaos.microservice.user.service.UserTagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
@Import({GlobalExceptionHandler.class, ResponseAdvice.class})
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
    private UserMapper userMapper;

    @MockBean
    private UserTagMapper userTagMapper;

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
