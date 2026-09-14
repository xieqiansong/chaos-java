package lan.chaos.sentinel.hotspot;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lan.chaos.sentinel.common.constant.SentinelConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 热点参数限流场景 — 对不同参数值设置不同 QPS 阈值。
 *
 * <p>程序化方式下，热点参数通过 {@code SphU.entry(resourceName, entryType, count, args)}
 * 传入参数值，Sentinel 自动匹配参数规则。</p>
 *
 * <p>本例规则：默认 QPS=2，参数值 "vip" 放宽至 QPS=100。
 * 典型生产场景：秒杀单商品限流、单 IP 防刷、VIP 用户白名单。</p>
 */
@Slf4j
@Service
public class HotspotService {

    /**
     * 热点参数限流 — productId 值影响 QPS 阈值。
     */
    public String purchase(String productId) {
        Entry entry = null;
        try {
            // 传入参数值，让 Sentinel 匹配热点参数规则
            entry = SphU.entry(SentinelConstants.HOTSPOT_PARAM, EntryType.OUT, 1, productId);
            log.info("[hotspot] productId={} 请求通过", productId);
            return "passed: " + productId;
        } catch (BlockException ex) {
            log.warn("[hotspot] productId={} 被热点限流: {}", productId, ex.getMessage());
            return "blocked: " + productId;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
