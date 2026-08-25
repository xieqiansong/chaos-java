package lan.chaos.ratelimiter.bench;

/**
 * 单次压测的运行参数（与 BenchRunner 的 BenchProperties 等价，独立出来供测试复用）。
 */
public class BenchOptions {

    /** 并发线程数 */
    public int threads = 8;

    /** 目标 QPS（flood=true 时忽略） */
    public int qps = 500;

    /** 租户全局限额 QPS */
    public long limit = 1000;

    /** 持续秒数 */
    public int durationSec = 10;

    /** 模拟集群节点数 */
    public int nodes = 4;

    /** 本地突发容量倍数（local-redis） */
    public double burstMultiplier = 1.5;

    /** 校准窗口毫秒（local-redis） */
    public long windowMs = 1000;

    /** 租户数 */
    public int tenants = 1;

    /** 满速（忽略 qps） */
    public boolean flood = false;

    /** 倾斜流量比例 0~1，-1=轮询 */
    public double skew = -1;

    public static BenchOptions of(int threads, int qps, long limit, int durationSec,
                                  int nodes, double burst, long windowMs, boolean flood, double skew) {
        BenchOptions o = new BenchOptions();
        o.threads = threads;
        o.qps = qps;
        o.limit = limit;
        o.durationSec = durationSec;
        o.nodes = nodes;
        o.burstMultiplier = burst;
        o.windowMs = windowMs;
        o.flood = flood;
        o.skew = skew;
        return o;
    }

    public String describe() {
        return String.format("threads=%d qps=%d limit=%d nodes=%d burst=%s window=%dms tenants=%d flood=%s skew=%s",
                threads, qps, limit, nodes, fmt(burstMultiplier), windowMs, tenants, flood, skew);
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }
}