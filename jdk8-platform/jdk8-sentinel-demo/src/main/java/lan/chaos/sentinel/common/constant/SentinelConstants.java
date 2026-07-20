package lan.chaos.sentinel.common.constant;

/**
 * Sentinel Demo 公共常量。
 * <p>资源名称集中管理，避免魔法字符串散落各 Service。</p>
 */
public final class SentinelConstants {

    private SentinelConstants() {
    }

    // ──────────── 流控资源名 ────────────
    /** QPS 直接限流资源 */
    public static final String FLOW_QPS = "flow-qps";
    /** WarmUp 冷启动/预热资源 */
    public static final String FLOW_WARMUP = "flow-warmup";
    /** 关联流控资源：关联资源被触发时，本资源被限流 */
    public static final String FLOW_REF = "flow-ref";
    /** 关联流控的"触发者"资源 */
    public static final String FLOW_REF_TRIGGER = "flow-ref-trigger";

    // ──────────── 熔断降级资源名 ────────────
    /** 异常比例熔断 */
    public static final String DEGRADE_EXCEPTION_RATIO = "degrade-exception-ratio";
    /** 异常数熔断 */
    public static final String DEGRADE_EXCEPTION_COUNT = "degrade-exception-count";
    /** 慢调用比例熔断 */
    public static final String DEGRADE_SLOW_RATIO = "degrade-slow-ratio";

    // ──────────── 热点参数资源名 ────────────
    /** 热点参数限流 */
    public static final String HOTSPOT_PARAM = "hotspot-param";

    // ──────────── 注解式资源 ────────────
    /** blockHandler 示例 */
    public static final String ANNO_BLOCK_HANDLER = "anno-block-handler";
    /** fallback 示例 */
    public static final String ANNO_FALLBACK = "anno-fallback";
    /** blockHandler + fallback 共存 */
    public static final String ANNO_BOTH = "anno-both";
}
