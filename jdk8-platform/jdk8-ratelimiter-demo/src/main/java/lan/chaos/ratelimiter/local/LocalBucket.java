package lan.chaos.ratelimiter.local;

/**
 * 线程安全令牌桶（基于时间戳补发，供节点内限流使用）。
 * 支持初始化与周期校准（重置速率/容量，保留未超容量的余量）。
 */
public final class LocalBucket {

    private double ratePerSec;
    private double capacity;
    private double tokens;
    private long lastNanos;
    private boolean initialized;
    private long served;

    /** 首次初始化。 */
    public synchronized void init(double ratePerSec, double capacity, long nowNanos) {
        this.ratePerSec = ratePerSec;
        this.capacity = capacity;
        this.tokens = capacity;
        this.lastNanos = nowNanos;
        this.initialized = true;
    }

    /**
     * 周期校准：按 Redis 分配的新配额重置速率与容量。
     * 语义 = 本期分到的配额直接可用（预填满 capacity），并随时间按 rate 补发超出容量部分。
     */
    public synchronized void calibrate(double ratePerSec, double capacity, long nowNanos) {
        this.ratePerSec = ratePerSec;
        this.tokens = capacity; // 本期分到的配额直接可用
        this.capacity = capacity;
        this.lastNanos = nowNanos;
        this.initialized = true;
    }

    /** 尝试取 1 个令牌，成功则计数。 */
    public synchronized boolean tryAcquire() {
        if (!initialized) {
            return false;
        }
        long now = System.nanoTime();
        long elapsed = now - lastNanos;
        if (elapsed > 0) {
            tokens = Math.min(capacity, tokens + ratePerSec * elapsed / 1_000_000_000.0);
            lastNanos = now;
        }
        if (tokens >= 1.0) {
            tokens -= 1.0;
            served++;
            return true;
        }
        return false;
    }

    /** 取走并清零本窗口已放行数（供上报 Redis）。 */
    public synchronized long takeAndResetServed() {
        long s = served;
        served = 0;
        return s;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }
}
