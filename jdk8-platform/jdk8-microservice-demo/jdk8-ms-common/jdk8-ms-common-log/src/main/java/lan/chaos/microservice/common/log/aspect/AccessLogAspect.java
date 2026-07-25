package lan.chaos.microservice.common.log.aspect;

import lan.chaos.microservice.common.core.constant.TraceConstants;
import lan.chaos.microservice.common.log.mask.SensitiveMasker;
import lan.chaos.microservice.common.log.properties.AccessLogProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Arrays;

/**
 * ★★★ P5 访问日志切面：环绕所有 {@code @RestController}，记录「谁在调用 / 入参 / 耗时 / 成败」，
 * 入参经 {@link SensitiveMasker} 脱敏后打印，满足「可观察且安全」。
 *
 * <p>WHY：每个对外接口都该有「输入→输出/耗时」可观察输出（本仓库硬约束），但手写日志既啰嗦又易漏。
 * 用 AOP 统一在切面里打印，业务方法零侵入。日志打到独立 logger {@code ACCESS_LOG}，
 * 方便生产环境单独归档 / 接入日志系统；traceId 取自 MDC 与全链路打通。</p>
 *
 * <p>注意：切点用 {@code within(@RestController *)}，只织入 Servlet 的 {@code @RestController}，
 * WebFlux 网关没有该注解故不受影响；框架对象（ServletRequest / BindingResult）由脱敏器输出类型占位。</p>
 */
@Aspect
public class AccessLogAspect {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");

    private final boolean includeArgs;

    public AccessLogAspect(AccessLogProperties properties) {
        this.includeArgs = properties.isIncludeArgs();
    }

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        String traceId = MDC.get(TraceConstants.TRACE_ID_MDC_KEY);
        String signature = pjp.getSignature().getDeclaringType().getSimpleName()
                + "#" + pjp.getSignature().getName();
        String args = includeArgs ? SensitiveMasker.mask(pjp.getArgs()) : "";

        long start = System.currentTimeMillis();
        boolean success = true;
        Throwable thrown = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            success = false;
            thrown = t;
            throw t;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            if (ACCESS_LOG.isInfoEnabled()) {
                ACCESS_LOG.info(format(traceId, signature, args, elapsed, success, thrown));
            }
        }
    }

    /**
     * 拼出一行访问日志（抽成静态方法，便于无 Spring 容器单测直接断言）。
     */
    static String format(String traceId, String signature, String args, long elapsedMs, boolean success, Throwable ex) {
        StringBuilder sb = new StringBuilder("[ACCESS] ");
        if (traceId != null && !traceId.isEmpty()) {
            sb.append("trace=").append(traceId).append(" ");
        }
        sb.append(signature).append(" ");
        if (args != null && !args.isEmpty()) {
            sb.append("args=").append(args).append(" ");
        }
        sb.append("elapsed=").append(elapsedMs).append("ms ");
        sb.append(success ? "OK" : "FAIL");
        if (ex != null) {
            sb.append(" ex=").append(ex.getClass().getSimpleName())
                    .append(":").append(ex.getMessage());
        }
        return sb.toString();
    }
}
