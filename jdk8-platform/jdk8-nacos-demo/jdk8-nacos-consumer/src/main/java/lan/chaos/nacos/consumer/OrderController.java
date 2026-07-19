package lan.chaos.nacos.consumer;

import lan.chaos.nacos.common.User;
import lan.chaos.nacos.common.UserClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * 演示两种服务发现调用方式，均通过"服务名"而非硬编码 IP：
 * <ul>
 *     <li>{@code /order/rest/{id}}：{@code @LoadBalanced RestTemplate}，手动拼服务名 URL</li>
 *     <li>{@code /order/feign/{id}}：声明式 {@link UserClient}，更简洁，推荐</li>
 * </ul>
 * 多实例启动 provider 时，连续调用可从返回的 {@code servedBy} 观察轮询负载均衡。
 */
@RestController
public class OrderController {

    private final RestTemplate restTemplate;

    private final UserClient userClient;

    public OrderController(RestTemplate restTemplate, UserClient userClient) {
        this.restTemplate = restTemplate;
        this.userClient = userClient;
    }

    @GetMapping("/order/rest/{id}")
    public User byRestTemplate(@PathVariable("id") Long id) {
        // 直接用服务名，由 LoadBalancer 解析为真实实例地址
        return restTemplate.getForObject("http://nacos-provider/user/" + id, User.class);
    }

    @GetMapping("/order/feign/{id}")
    public User byFeign(@PathVariable("id") Long id) {
        return userClient.getById(id);
    }
}
