package lan.chaos.microservice.common.feign.fallback;

import feign.hystrix.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feign 熔断降级工厂基类。
 *
 * <p>WHY：Sentinel 接管 Feign 后，下游服务抛异常、或被“慢调用比例 / 异常比例”熔断时，
 * 会走 fallback 而不是让异常一路向上炸穿调用方。每个 Feign Client 配一个 FallbackFactory，
 * 既能拿到 {@link Throwable} 记录证据，又能返回友好的 {@code R} 兜底，避免“一个服务挂、整条链路雪崩”。</p>
 *
 * <p>用法：继承本类，实现 {@link #targetService()} 与 {@link #createFallback(Throwable)}，
 * 在 {@code createFallback} 里用 {@link FallbackResults#degraded()} 返回降级响应。</p>
 */
public abstract class AbstractFallbackFactory<T> implements FallbackFactory<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public T create(Throwable cause) {
        log.warn("[Feign-Fallback] 调用下游服务[{}]失败，已降级。reason={}",
                targetService(), cause == null ? "unknown" : cause.getMessage());
        return createFallback(cause);
    }

    /** 返回降级实现：方法内用 {@link FallbackResults#degraded()} 返回兜底 {@code R} */
    protected abstract T createFallback(Throwable cause);

    /** 被保护的下游服务名（仅用于日志，例如 "ms-user"） */
    protected abstract String targetService();
}
