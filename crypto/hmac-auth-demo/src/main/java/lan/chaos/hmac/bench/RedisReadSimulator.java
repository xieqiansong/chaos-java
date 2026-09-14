package lan.chaos.hmac.bench;

import java.util.concurrent.locks.LockSupport;

/**
 * 模拟集中式鉴权（如 Redis 读 Token 校验）的一次网络往返延迟。
 * 默认约 1ms，可配。仅用于场景 D 吞吐对比演示，不依赖真实 Redis。
 */
public final class RedisReadSimulator {

    private final long latencyNanos;

    /**
     * @param latencyMicros 模拟往返延迟（微秒），默认场景传 1000（≈1ms）
     */
    public RedisReadSimulator(long latencyMicros) {
        this.latencyNanos = latencyMicros * 1000L;
    }

    /** 模拟一次 Redis 读耗时。 */
    public void read() {
        LockSupport.parkNanos(latencyNanos);
    }
}
