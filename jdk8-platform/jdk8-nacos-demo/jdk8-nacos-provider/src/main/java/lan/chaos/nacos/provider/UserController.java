package lan.chaos.nacos.provider;

import lan.chaos.nacos.common.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外暴露 {@code /user/{id}} 接口。
 *
 * <p>返回体里带上本实例的端口（{@code servedBy}），这样当以不同端口启动多个 provider 实例时，
 * 消费端连续调用即可从 {@code servedBy} 的变化直观看到负载均衡（轮询）效果。</p>
 */
@RestController
public class UserController {

    @Value("${server.port}")
    private String port;

    @GetMapping("/user/{id}")
    public User getById(@PathVariable("id") Long id) {
        return new User(id, "user-" + id, "provider:" + port);
    }
}
