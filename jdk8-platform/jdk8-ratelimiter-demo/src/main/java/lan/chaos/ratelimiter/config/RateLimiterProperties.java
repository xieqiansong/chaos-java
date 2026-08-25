package lan.chaos.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流器运行配置（前缀 ratelimiter）。
 *
 * <p>对应 application.yml 的 {@code ratelimiter:} 节。mode 决定 REST 演示接口默认使用哪种实现。
 */
@ConfigurationProperties(prefix = "ratelimiter")
public class RateLimiterProperties {

    /** 默认实现：local-redis | redis-lua | local-only */
    private String mode = "local-redis";

    /** 租户全局限流 QPS */
    private long defaultQps = 1000;

    /** 本地+Redis 校准窗口（毫秒） */
    private long windowMs = 1000;

    /** 本地突发容量倍数（local-redis） */
    private double burstMultiplier = 1.5;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getDefaultQps() {
        return defaultQps;
    }

    public void setDefaultQps(long defaultQps) {
        this.defaultQps = defaultQps;
    }

    public long getWindowMs() {
        return windowMs;
    }

    public void setWindowMs(long windowMs) {
        this.windowMs = windowMs;
    }

    public double getBurstMultiplier() {
        return burstMultiplier;
    }

    public void setBurstMultiplier(double burstMultiplier) {
        this.burstMultiplier = burstMultiplier;
    }
}