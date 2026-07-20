package lan.chaos.sentinel.flow;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lan.chaos.sentinel.common.constant.SentinelConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流控场景 Service — 使用 {@link SphU#entry(String)} 程序化资源定义。
 *
 * <p>与 {@code @SentinelResource} 注解方式相比，程序化方式：
 * <ul>
 *   <li>不需要 Spring AOP，不依赖代理，调用链路可控</li>
 *   <li>{@code try-catch BlockException} 后可在同一方法内写降级逻辑，
 *       不像 blockHandler 必须单独写一个方法</li>
 *   <li>需要手动在 {@code finally} 中 {@code entry.exit()}，否则资源计数泄露</li>
 * </ul>
 *
 * <h3>流控模式</h3>
 * <ul>
 *   <li><b>直接</b>：对本资源 QPS 限流</li>
 *   <li><b>关联</b>：关联资源压力大时连带限流当前资源</li>
 *   <li><b>WarmUp</b>：冷启动时缓慢爬升阈值，避免瞬时流量打垮新启动的节点</li>
 * </ul>
 */
@Slf4j
@Service
public class FlowControlService {

    private final AtomicInteger passedCount = new AtomicInteger(0);
    private final AtomicInteger blockedCount = new AtomicInteger(0);

    // ==================== QPS 直接限流 ====================

    /**
     * QPS 直接限流 — count=2，超限抛出 {@link BlockException}。
     */
    public String qpsLimited() {
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelConstants.FLOW_QPS);
            passedCount.incrementAndGet();
            log.info("[flow-qps] 请求通过");
            return "passed";
        } catch (BlockException ex) {
            blockedCount.incrementAndGet();
            log.warn("[flow-qps] 被限流: {}", ex.getMessage());
            return "blocked";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    // ==================== WarmUp 预热 ====================

    /**
     * WarmUp 预热限流 — {@code warmUpPeriodSec=10, count=100}。
     * <p>默认阈值较高，正常调用不会触发限流。测试中动态降低 count 来验证预热效果。</p>
     */
    public String warmUpLimited() {
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelConstants.FLOW_WARMUP);
            log.info("[flow-warmup] 请求通过");
            return "passed";
        } catch (BlockException ex) {
            log.warn("[flow-warmup] 预热期被限流: {}", ex.getMessage());
            return "blocked";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    // ==================== 关联限流 ====================

    /**
     * 关联限流中的"被保护"资源 — 当 trigger QPS 过高时，本资源被连带限流。
     */
    public String refProtected() {
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelConstants.FLOW_REF);
            log.info("[flow-ref] 请求通过");
            return "passed";
        } catch (BlockException ex) {
            log.warn("[flow-ref] 被关联限流: {}", ex.getMessage());
            return "blocked";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    /**
     * 关联限流的"触发者" — 高频调用后 {@code refProtected()} 被连带限流。
     */
    public String refTrigger() {
        // 这里也走 SphU.entry 以便 Sentinel 统计 QPS
        Entry entry = null;
        try {
            entry = SphU.entry(SentinelConstants.FLOW_REF_TRIGGER);
            log.info("[flow-ref-trigger] 制造压力");
            return "triggered";
        } catch (BlockException ex) {
            return "triggered";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    // ==================== 统计 ====================

    public int getPassedCount() {
        return passedCount.get();
    }

    public int getBlockedCount() {
        return blockedCount.get();
    }
}
