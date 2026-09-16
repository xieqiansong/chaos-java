package lan.chaos.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要「计时 / 日志增强」的方法。
 * 既可用于接口方法，也可用于实现类方法，代理侧通过反射读取。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Measured {
    String value() default "";
}
