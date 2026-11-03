package lan.chaos.dubbo.consumer.demo.facade;

import lan.chaos.dubbo.api.UserService;
import lan.chaos.dubbo.api.common.model.User;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 基础同步调用 facade（点对点直连 20880）。
 */
@Component
public class BasicUserFacade {

    @DubboReference(url = "dubbo://127.0.0.1:20880", group = "basic")
    private UserService userService;


    public User getById(Long id) {
        return userService.getById(id);
    }

    /** 可观察输出：输入→输出。 */
    public String describe(Long id) {
        User u = getById(id);
        return "getById(" + id + ") -> " + u + "  (由实例 " + u.getInstanceId() + " 处理)";
    }
}
