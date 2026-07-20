package lan.chaos.sentinel.degrade;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lan.chaos.sentinel.common.constant.SentinelConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 熔断降级场景 Service — 使用 {@link SphU#entry(String)} + {@link Tracer#trace(Throwable)}。
 *
 * <p>{@code Tracer.trace(ex)} 是 Sentinel 统计异常的关键 API：
 * 只有被 trace 的异常才会计入熔断决策的异常数/异常比例。直接抛异常不走 trace 不会被统计。</p>
 *
 * <h3>熔断状态机</h3>
 * <pre>
 * CLOSED → (阈值触发) → OPEN → (timeWindow 到期) → HALF_OPEN → (探测成功) → CLOSED
 *                           ↑                              │
 *                           └── (探测失败) ─────────────────┘
 * </pre>
 */
@Slf4j
@Service
public class DegradeService {

    // ==================== 异常比例熔断 ====================

    /**
     * 异常比例熔断 — 窗口内异常比例 > 0.5 且请求数 ≥ 3 → 熔断。
     * <p>{@code throwEx=true} 时抛出业务异常并通过 {@code Tracer.trace()} 统计。</p>
     */
    public String exceptionRatio(boolean throwEx) {
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelConstants.DEGRADE_EXCEPTION_RATIO);
            if (throwEx) {
                RuntimeException ex = new RuntimeException("模拟业务异常");
                Tracer.trace(ex); // 计入熔断统计
                throw ex;
            }
            log.info("[degrade-exception-ratio] 正常返回");
            return "passed";
        } catch (BlockException ex) {
            log.warn("[degrade-exception-ratio] 熔断打开: {}", ex.getMessage());
            return "degraded";
        } catch (RuntimeException ex) {
            log.warn("[degrade-exception-ratio] 业务异常: {}", ex.getMessage());
            return "fallback";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    // ==================== 异常数熔断 ====================

    /**
     * 异常数熔断 — 窗口内异常数 ≥ 3 → 熔断。
     */
    public String exceptionCount(boolean throwEx) {
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelConstants.DEGRADE_EXCEPTION_COUNT);
            if (throwEx) {
                RuntimeException ex = new RuntimeException("模拟业务异常");
                Tracer.trace(ex);
                throw ex;
            }
            log.info("[degrade-exception-count] 正常返回");
            return "passed";
        } catch (BlockException ex) {
            log.warn("[degrade-exception-count] 熔断打开: {}", ex.getMessage());
            return "degraded";
        } catch (RuntimeException ex) {
            log.warn("[degrade-exception-count] 业务异常: {}", ex.getMessage());
            return "fallback";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    // ==================== 慢调用比例熔断 ====================

    /**
     * 慢调用比例熔断 — 窗口内 RT > 200ms 的比例 > 0.5 且请求数 ≥ 3 → 熔断。
     */
    public String slowCallRatio(long sleepMs) {
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelConstants.DEGRADE_SLOW_RATIO);
            Thread.sleep(sleepMs);
            log.info("[degrade-slow-ratio] RT={}ms 返回", sleepMs);
            return "passed";
        } catch (BlockException ex) {
            log.warn("[degrade-slow-ratio] 熔断打开: {}", ex.getMessage());
            return "degraded";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
