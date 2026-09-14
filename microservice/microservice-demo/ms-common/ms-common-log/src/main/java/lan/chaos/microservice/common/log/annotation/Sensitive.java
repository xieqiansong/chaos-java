package lan.chaos.microservice.common.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记字段为「敏感字段」：访问日志脱敏时会把该字段的值替换为 {@code ******}。
 *
 * <p>WHY：访问日志要把入参打出来排障，但密码 / 密钥一旦落盘就是安全事故。
 * 除了按字段名自动命中（password/token/secret…），也可用本注解显式标注任意字段，
 * 不依赖名字约定，最可靠。例：登录请求的 {@code password} 字段。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
}
