package lan.chaos.sentinel.anno;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lan.chaos.sentinel.common.constant.SentinelConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @SentinelResource 注解用法场景 — 演示 blockHandler / fallback / defaultFallback。
 *
 * <h3>三者关系</h3>
 * <pre>
 * 发生异常
 *   ├── 是 BlockException → blockHandler（如果配置）
 *   │     ├── 方法级 blockHandler
 *   │     └── 类级 defaultBlockHandlerClass（未演示）
 *   └── 非 BlockException → fallback（如果配置）
 *         ├── 方法级 fallback
 *         └── 类级 defaultFallback
 * </pre>
 *
 * <p>关键点：{@code blockHandler} 和 {@code fallback} 可以共存，
 * 且 {@code blockHandler} 可以调用 {@code fallback} 做统一降级输出。</p>
 *
 * <p>生产化要点：blockHandler 通常返回静态兜底数据或提示；
 * fallback 记录异常日志并返回友好提示。</p>
 */
@Slf4j
@Service
public class SentinelAnnotationService {

    // ==================== 只有 blockHandler ====================

    /**
     * 只配 blockHandler，不配 fallback — 限流走降级，业务异常直接抛出。
     */
    @SentinelResource(value = SentinelConstants.ANNO_BLOCK_HANDLER,
            blockHandler = "annoBlockHandler")
    public String onlyBlockHandler(boolean throwEx) {
        if (throwEx) {
            throw new RuntimeException("业务异常，blockHandler 不处理");
        }
        log.info("[anno-block] 请求通过");
        return "passed";
    }

    public String annoBlockHandler(BlockException ex) {
        log.warn("[anno-block] 被限流: {}", ex.getMessage());
        return "blocked";
    }

    // ==================== 只有 fallback ====================

    /**
     * 只配 fallback，不配 blockHandler — 业务异常走降级，限流则抛 BlockException。
     */
    @SentinelResource(value = SentinelConstants.ANNO_FALLBACK,
            fallback = "annoFallback")
    public String onlyFallback(boolean throwEx) {
        if (throwEx) {
            throw new RuntimeException("业务异常，被 fallback 捕获");
        }
        log.info("[anno-fallback] 请求通过");
        return "passed";
    }

    public String annoFallback(Throwable t) {
        log.warn("[anno-fallback] 降级: {}", t.getMessage());
        return "fallback: " + t.getMessage();
    }

    // ==================== blockHandler + fallback 共存 ====================

    /**
     * 同时配置 blockHandler 和 fallback — 分别处理限流和业务异常。
     * <p>这是生产最推荐的配置方式，两种异常各有归宿。</p>
     */
    @SentinelResource(value = SentinelConstants.ANNO_BOTH,
            blockHandler = "bothBlockHandler",
            fallback = "bothFallback")
    public String bothHandlers(boolean throwEx) {
        if (throwEx) {
            throw new RuntimeException("业务异常");
        }
        log.info("[anno-both] 请求通过");
        return "passed";
    }

    public String bothBlockHandler(BlockException ex) {
        log.warn("[anno-both] 被限流: {}", ex.getMessage());
        return "blocked";
    }

    public String bothFallback(Throwable t) {
        log.warn("[anno-both] 异常降级: {}", t.getMessage());
        return "fallback: " + t.getMessage();
    }
}
