package lan.chaos.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「不希望被代理拦截」的方法（用于 CGLib 的 CallbackFilter 路由到 NoOp）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NoIntercept {
}
