package lan.chaos.ratelimiter.local;

import lan.chaos.ratelimiter.RateLimiter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 优化方案：本地令牌桶 + Redis 周期校准。
 *
 * <p>设计：
 * <ul>
 *   <li>第一层（本地）：每节点每租户一个内存令牌桶，热路径零网络；</li>
 *   <li>第二层（Redis）：全局窗口计数器为权威，节点每 {@code windowMs} 校准一次；</li>
 *   <li>校准返回本节点下一窗口配额 = 全局剩余 / N，本地容量 = 配额 × burstMultiplier。</li>
 * </ul>
 *
 * <p>精度损失上界：N 节点同时打满本地桶时，全局超限 ≈ (burstMultiplier - 1) × 窗口配额。
 * burst=1 零超限但流量倾斜时欠用。
 *
 * <p>多实例（模拟集群节点）共享同一 Redis 计数器 key（rl2:cnt:{tenant}），各持有独立本地桶。
 */
public final class LocalRedisRateLimiter implements RateLimiter {

    /** 校准脚本见 resources/lua/rebalance.lua。 */
    private static final DefaultRedisScript<String> SCRIPT = new DefaultRedisScript<>();
    static {
        SCRIPT.setLocation(new ClassPathResource("lua/rebalance.lua"));
        SCRIPT.setResultType(String.class);
    }

    private final StringRedisTemplate redis;
    private final String nodeId;
    private final double globalQps;
    private final long windowMs;
    private final int nodeCount;
    private final double burstMultiplier;

    private final ConcurrentHashMap<String, LocalBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong redisCalls = new AtomicLong();
    private final AtomicLong localAllows = new AtomicLong();

    private final ScheduledExecutorService scheduler;

    public LocalRedisRateLimiter(StringRedisTemplate redis, String nodeId,
                                 double globalQps, long windowMs, int nodeCount, double burstMultiplier) {
        this.redis = redis;
        this.nodeId = nodeId;
        this.globalQps = globalQps;
        this.windowMs = windowMs;
        this.nodeCount = nodeCount;
        this.burstMultiplier = burstMultiplier;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rl-sync-" + nodeId);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::syncAll, windowMs, windowMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean tryAcquire(String tenantId) {
        LocalBucket b = buckets.computeIfAbsent(tenantId, t -> new LocalBucket());
        if (!b.isInitialized()) {
            calibrate(tenantId, b);
        }
        if (b.tryAcquire()) {
            localAllows.incrementAndGet();
            return true;
        }
        return false;
    }

    /** 周期校准所有已知租户。校准失败保留上一窗口配额，下个窗口重试。 */
    private void syncAll() {
        for (Map.Entry<String, LocalBucket> e : buckets.entrySet()) {
            try {
                calibrate(e.getKey(), e.getValue());
            } catch (RuntimeException ignored) {
                // Redis 抖动：保持当前本地配额，下一窗口重试
            }
        }
    }

    /** 向 Redis 上报本窗口消耗，取回下一窗口配额并重置本地桶。 */
    private void calibrate(String tenantId, LocalBucket b) {
        long consumed = b.takeAndResetServed();
        long now = System.currentTimeMillis();
        double windowQuota = globalQps * windowMs / 1000.0;
        redisCalls.incrementAndGet();
        String val = redis.execute(SCRIPT, Collections.singletonList("rl2:cnt:" + tenantId),
                String.valueOf(windowMs), String.valueOf(windowQuota),
                String.valueOf(nodeCount), String.valueOf(consumed), String.valueOf(now));
        double allocated = (val == null || val.isEmpty()) ? 0.0 : Double.parseDouble(val);
        double rate = allocated * 1000.0 / windowMs;      // 个/s
        double capacity = allocated * burstMultiplier;    // 本地突发容量
        b.calibrate(rate, capacity, System.nanoTime());
    }

    /** 关闭后台校准线程（容器销毁前调用）。 */
    public void close() {
        scheduler.shutdownNow();
    }

    @Override
    public String name() {
        return "local-redis";
    }

    @Override
    public long redisCalls() {
        return redisCalls.get();
    }

    @Override
    public long localAllows() {
        return localAllows.get();
    }
}