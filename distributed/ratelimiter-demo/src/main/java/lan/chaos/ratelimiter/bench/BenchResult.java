package lan.chaos.ratelimiter.bench;

/**
 * 压测一次运行的结构化结果（供 BenchRunner 打印 / SpringBootTest 汇总）。
 */
public class BenchResult {

    public final long totalReq;
    public final long allowed;
    public final long denied;
    public final double qps;
    public final double avgUs;
    public final double p99Us;
    public final long redisCalls;
    public final double redisPerSec;
    public final long localAllows;
    public final double localHitPct;
    public final double overLimitPct;

    public BenchResult(long totalReq, long allowed, long denied, double qps,
                       double avgUs, double p99Us, long redisCalls, double redisPerSec,
                       long localAllows, double localHitPct, double overLimitPct) {
        this.totalReq = totalReq;
        this.allowed = allowed;
        this.denied = denied;
        this.qps = qps;
        this.avgUs = avgUs;
        this.p99Us = p99Us;
        this.redisCalls = redisCalls;
        this.redisPerSec = redisPerSec;
        this.localAllows = localAllows;
        this.localHitPct = localHitPct;
        this.overLimitPct = overLimitPct;
    }
}