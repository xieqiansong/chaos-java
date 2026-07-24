package lan.chaos.microservice.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级权限注解（P4 细粒度授权）。
 *
 * <p>打在 Controller 方法上，由 {@code PermissionInterceptor} 在请求进入时校验当前登录用户是否具备所需权限：
 * <ul>
 *   <li>{@code @RequiresPermission("user:read")} —— 需含该权限；</li>
 *   <li>{@code @RequiresPermission(value={"user:read","user:write"}, logical=Logical.AND)}
 *       —— 默认 AND，需同时具备；</li>
 *   <li>{@code @RequiresPermission(value={"a","b"}, logical=Logical.OR)} —— 具备其一即可。</li>
 * </ul>
 *
 * <p>注意：注解只做「细粒度授权」，粗粒度「是否登录/有无合法 token」由网关统一把关。
 * 下游服务即便跳过注解校验，也拿不到伪造的用户身份（token 已在网关验过签名）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /** 所需权限标识集合。 */
    String[] value();

    /** 多权限间的逻辑关系，默认 AND（全部满足）。 */
    Logical logical() default Logical.AND;
}
