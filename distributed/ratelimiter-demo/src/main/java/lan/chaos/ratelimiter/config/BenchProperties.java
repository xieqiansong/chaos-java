package lan.chaos.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 压测参数（前缀 ratelimiter.bench）。通过命令行 --ratelimiter.bench.*= 覆盖。
 * 启动时 --ratelimiter.bench.enabled=true 则立即跑压测、打印结果后退出。
 */
@ConfigurationProperties(prefix = "ratelimiter.bench")
public class BenchProperties {

    /** 是否启用压测模式（启动即压力测试，跑完退出） */
    private boolean enabled = false;

    /** 实现模式：local-redis | redis-lua | local-only */
    private String mode = "local-redis";

    /** 压测线程数 */
    private int threads = 8;

    /** 目标请求 QPS（flood 时忽略） */
    private int qps = 500;

    /** 租户全局限额 QPS */
    private long limit = 1000;

    /** 压测持续秒数 */
    private int durationSec = 10;

    /** 模拟集群节点数 */
    private int nodes = 4;

    /** 本地突发容量倍数（local-redis） */
    private double burstMultiplier = 1.5;

    /** 校准窗口（毫秒，local-redis） */
    private long windowMs = 1000;

    /** 租户数 */
    private int tenants = 1;

    /** 满速压测（忽略 qps） */
    private boolean flood = false;

    /** 倾斜：打到 node0 的流量比例 0~1；-1 表示轮询 */
    private double skew = -1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getQps() {
        return qps;
    }

    public void setQps(int qps) {
        this.qps = qps;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(int durationSec) {
        this.durationSec = durationSec;
    }

    public int getNodes() {
        return nodes;
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    public double getBurstMultiplier() {
        return burstMultiplier;
    }

    public void setBurstMultiplier(double burstMultiplier) {
        this.burstMultiplier = burstMultiplier;
    }

    public long getWindowMs() {
        return windowMs;
    }

    public void setWindowMs(long windowMs) {
        this.windowMs = windowMs;
    }

    public int getTenants() {
        return tenants;
    }

    public void setTenants(int tenants) {
        this.tenants = tenants;
    }

    public boolean isFlood() {
        return flood;
    }

    public void setFlood(boolean flood) {
        this.flood = flood;
    }

    public double getSkew() {
        return skew;
    }

    public void setSkew(double skew) {
        this.skew = skew;
    }
}