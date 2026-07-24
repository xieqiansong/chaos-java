package lan.chaos.microservice.common.security.context;

import lan.chaos.microservice.common.security.model.LoginUser;

/**
 * 登录用户上下文（ThreadLocal）。
 *
 * <p>由 {@code PermissionInterceptor} 在请求进入时 set，请求结束后 clear（防止线程复用串号）。
 * Controller/Service 里用 {@code LoginUserContext.get()} 即可拿到当前用户，无需层层传参。</p>
 */
public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 可能为 null：公开接口（无 @RequiresPermission）且未带有效 token 时并不强制登录。 */
    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private LoginUserContext() {
    }
}
