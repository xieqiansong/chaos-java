package lan.chaos.ratelimiter;

/**
 * 限流器统一接口（租户维度）。
 * 各实现需提供压测指标：Redis 调用次数、本地放行次数。
 */
public interface RateLimiter {

    /** 尝试获取 1 个许可，成功返回 true。 */
    boolean tryAcquire(String tenantId);

    /** 实现标识（用于压测输出）。 */
    String name();

    /** Redis 调用次数（压测指标）。 */
    long redisCalls();

    /** 本地放行次数（压测指标，纯本地实现为 0）。 */
    long localAllows();
}
