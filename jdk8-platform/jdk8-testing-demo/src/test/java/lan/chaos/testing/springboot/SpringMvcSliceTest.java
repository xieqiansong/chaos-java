package lan.chaos.testing.springboot;

import lan.chaos.testing.common.model.User;
import lan.chaos.testing.common.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 能力五：Spring Boot 切片测试 —— @WebMvcTest + MockMvc。
 */
@WebMvcTest(lan.chaos.testing.common.controller.UserController.class)
class SpringMvcSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getUserName_shouldReturn200() throws Exception {
        given(userService.getUserName(1L)).willReturn("张三");

        mockMvc.perform(get("/users/1/name"))
                .andExpect(status().isOk())
                .andExpect(content().string("张三"));

        verify(userService).getUserName(1L);
    }

    @Test
    void createUser_shouldReturnUserJson() throws Exception {
        User mockUser = User.builder().id(100L).name("新用户").email("new@test.com").build();
        given(userService.createUser("新用户", "new@test.com")).willReturn(mockUser);

        mockMvc.perform(post("/users")
                        .param("name", "新用户")
                        .param("email", "new@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("新用户"))
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    void countUsers_shouldReturn200() throws Exception {
        given(userService.countUsers()).willReturn(0L);

        mockMvc.perform(get("/users/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
