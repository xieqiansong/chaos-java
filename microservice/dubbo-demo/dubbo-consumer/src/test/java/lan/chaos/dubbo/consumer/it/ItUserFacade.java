package lan.chaos.dubbo.consumer.it;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 注册中心形态的引用 facade（仅用于 *IT 集成测试）：不写 url，表示从 Nacos 发现 provider 实例。
 */
@Component
public class ItUserFacade {

    @DubboReference
    private UserService userService;

    public User getById(Long id) {
        return userService.getById(id);
    }
}
