package lan.chaos.ratelimiter.local;

import lan.chaos.ratelimiter.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 参考实现：纯本地令牌桶。无跨节点约束，性能下界参考。
 * 每个租户一个令牌桶，速率/容量固定（= 全局限额）。
 * 生产上不可用于多节点集群——多节点各自满额会按节点数成倍超限。
 */
public final class LocalOnlyRateLimiter implements RateLimiter {

    private final double ratePerSec;
    private final double capacity;
    private final ConcurrentHashMap<String, LocalBucket> buckets = new ConcurrentHashMap<>();

    public LocalOnlyRateLimiter(double ratePerSec, double capacity) {
        this.ratePerSec = ratePerSec;
        this.capacity = capacity;
    }

    @Override
    public boolean tryAcquire(String tenantId) {
        LocalBucket b = buckets.computeIfAbsent(tenantId, t -> {
            LocalBucket nb = new LocalBucket();
            nb.init(ratePerSec, capacity, System.nanoTime());
            return nb;
        });
        return b.tryAcquire();
    }

    @Override
    public String name() {
        return "local-only";
    }

    @Override
    public long redisCalls() {
        return 0;
    }

    @Override
    public long localAllows() {
        return 0;
    }
}
