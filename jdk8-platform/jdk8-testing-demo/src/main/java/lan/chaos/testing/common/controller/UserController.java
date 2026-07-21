package lan.chaos.testing.common.controller;

import lan.chaos.testing.common.model.User;
import lan.chaos.testing.common.service.UserService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器 — 用于 Spring Boot 切片测试（@WebMvcTest）。
 *
 * <p>仅提供最简 REST 端点，演示 @WebMvcTest + MockMvc 测试。
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}/name")
    public String getUserName(@PathVariable Long id) {
        return userService.getUserName(id);
    }

    @PostMapping
    public User createUser(@RequestParam String name, @RequestParam String email) {
        return userService.createUser(name, email);
    }

    @GetMapping("/count")
    public long countUsers() {
        return userService.countUsers();
    }
}
