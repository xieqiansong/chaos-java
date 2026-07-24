package lan.chaos.microservice.common.core.constant;

/**
 * 链路追踪相关常量（跨进程透传的唯一 ID）。
 *
 * <p>WHY：网关/上游生成的 traceId 必须在 MDC（日志）与 HTTP Header（跨服务透传）保持同一名字，
 * 否则排查问题时会因为“名字对不上”而丢失链路。这里集中定义，common-log 写入、Feign 透传都引用它，
 * 与 {@code application-common.yml} 中的 {@code trace.header} 保持完全一致。</p>
 */
public final class TraceConstants {

    /** MDC 中的 key，由 common-log 的 TraceIdFilter 在请求入口写入 */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /** 跨服务透传的 HTTP Header 名 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceConstants() {
    }
}
