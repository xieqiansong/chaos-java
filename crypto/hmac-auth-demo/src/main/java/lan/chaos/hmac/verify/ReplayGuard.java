package lan.chaos.hmac.verify;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防重放：滑动时间窗频率限制 + 业务幂等键去重。全程内存计算，0 网络往返。
 *
 * <p>设计要点：
 * <ul>
 *   <li><b>频率限制</b>：每设备滑动窗口内最多 {@code windowMax} 次，仅内存计数；
 *       窗口移动后计数自动归零，不存 nonce 原文。</li>
 *   <li><b>幂等去重</b>：{@code deviceId+batchNo} 唯一键（生产用 DB 唯一索引兜底，
 *       这里用内存 Map 模拟写侧去重）。重放危害是「数据重复写」，在写侧去重
 *       比在鉴权链路为每个正常请求预支一次存储读更划算。</li>
 * </ul>
 *
 * <p>为什么不上 Redis / 内存 nonce：
 * <ul>
 *   <li>Redis {@code SETNX} 存 nonce = 每请求一次网络往返，把「读 Token」换成
 *       「读 nonce」，瓶颈原地搬家，违背 HMAC 无状态改造初衷。</li>
 *   <li>内存 Map 存 nonce：1200 万设备 × ≈216B/条 ≈ 2.6GB；跨节点需 IP/设备哈希
 *       吸附，而终端设备 NAT 出口 IP 聚集会制造单点热点。</li>
 * </ul>
 *
 * <p>生产分档：高频上报 = 时间窗 + 幂等兜底（0 往返）；低频敏感操作 = nonce +
 * Redis SETNX（分钟级频率，一次往返值得）。
 */
public final class ReplayGuard {

    private final int windowMax;
    private final long windowMillis;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final Map<String, Long> idempotencyKeys = new ConcurrentHashMap<>();

    public ReplayGuard(int windowMax, long windowMillis) {
        this.windowMax = windowMax;
        this.windowMillis = windowMillis;
    }

    /** 滑动窗口频率限制：返回 false 表示窗口内超频。并发下用 CAS 重试保证计数不丢。 */
    public boolean allow(String deviceId, long nowMillis) {
        WindowCounter current = counters.get(deviceId);
        if (current == null) {
            WindowCounter next = new WindowCounter(nowMillis, 1);
            WindowCounter prev = counters.putIfAbsent(deviceId, next);
            return prev == null || prev.count <= windowMax;
        }
        // CAS 更新：窗口滚动则重置计数，否则 +1
        for (; ; ) {
            WindowCounter fresh = counters.get(deviceId);
            if (fresh == null) {
                WindowCounter next = new WindowCounter(nowMillis, 1);
                if (counters.putIfAbsent(deviceId, next) == null) {
                    return true;
                }
                continue;
            }
            WindowCounter updated = fresh.incr(nowMillis, windowMillis);
            if (counters.replace(deviceId, fresh, updated)) {
                return updated.count <= windowMax;
            }
        }
    }

    /** 业务幂等去重：同一 deviceId+batchNo 首次返回 false，重复返回 true。 */
    public boolean isDuplicate(String deviceId, String batchNo) {
        return idempotencyKeys.putIfAbsent(deviceId + ":" + batchNo,
                System.currentTimeMillis()) != null;
    }

    /** 滑动窗口计数器（不可变，便于 CAS）。 */
    private static final class WindowCounter {
        final long windowStart;
        final int count;

        WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }

        WindowCounter incr(long nowMillis, long windowMillis) {
            if (nowMillis - windowStart >= windowMillis) {
                return new WindowCounter(nowMillis, 1);
            }
            return new WindowCounter(windowStart, count + 1);
        }
    }
}
