package lan.chaos.microservice.user.controller;

import lan.chaos.microservice.common.core.result.R;
import lan.chaos.microservice.common.security.annotation.RequiresPermission;
import lan.chaos.microservice.common.security.context.LoginUserContext;
import lan.chaos.microservice.common.security.model.LoginUser;
import lan.chaos.microservice.user.entity.User;
import lan.chaos.microservice.user.entity.UserTag;
import lan.chaos.microservice.user.model.AddTagRequest;
import lan.chaos.microservice.user.model.CreateUserRequest;
import lan.chaos.microservice.user.service.AccountService;
import lan.chaos.microservice.user.service.UserService;
import lan.chaos.microservice.user.service.UserTagService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

/**
 * 用户服务 HTTP 入口（仅做“参数接收 + 委托 service”，不含业务知识点）。
 *
 * <p>所有返回都会被 common-web 的 ResponseAdvice 包成 {@code R<T>}。
 * 演示：用户写在主库 PG，标签写在副库 MySQL，但调用方无感——这就是多数据源的价值。</p>
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private UserTagService userTagService;

    @Resource
    private AccountService accountService;

    @PostMapping
    public User create(@RequestBody @Valid CreateUserRequest req) {
        return userService.createUser(req.getUsername(), req.getNickname(), req.getAge(), req.getPhone());
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @GetMapping
    public List<User> list() {
        return userService.listUsers();
    }

    @PostMapping("/{id}/tags")
    public UserTag addTag(@PathVariable Long id, @RequestBody @Valid AddTagRequest req) {
        return userTagService.addTag(id, req.getTag());
    }

    @GetMapping("/{id}/tags")
    public List<UserTag> listTags(@PathVariable Long id) {
        return userTagService.listTags(id);
    }

    /**
     * 扣减账户余额（Seata 全局事务的「用户侧分支资源」）。
     *
     * <p>通常只被订单服务在 {@code @GlobalTransactional} 内通过 Feign 调用；xid 由 Seata 自动透传。
     * 也可单独 curl 演示：余额不足时返回 {@code 409}（BALANCE_NOT_ENOUGH）。</p>
     */
    @PostMapping("/{id}/account/deduct")
    public void deductAccount(@PathVariable("id") Long id, @RequestParam("amount") BigDecimal amount) {
        accountService.deduct(id, amount);
    }

    /**
     * P4 细粒度授权演示①：返回「当前登录用户是谁」。
     *
     * <p>打 {@code @RequiresPermission("user:read")}，由 PermissionInterceptor 从网关透传来的
     * Authorization 头解析 JWT，把身份放进 {@link LoginUserContext}；这里直接读出来返回，
     * 证明下游服务无需查库就拿到了网关验过的身份（输入：token → 输出：userId/username/permissions）。</p>
     */
    @RequiresPermission("user:read")
    @GetMapping("/me")
    public R<LoginUser> me() {
        return R.ok(LoginUserContext.get());
    }

    /**
     * P4 细粒度授权演示②：删除用户（仅演示权限拦截，不真删数据）。
     *
     * <p>打 {@code @RequiresPermission("user:write")}：admin（有 user:write）可调，guest（仅 user:read）会被拦截器返回 403。
     * 用来和 {@code /me} 对照，看「权限不足」与「权限足够」的差异。</p>
     */
    @RequiresPermission("user:write")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        return R.ok();
    }
}
