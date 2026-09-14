package lan.chaos.batchwriter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 压测参数（前缀 batchingest.bench）。通过命令行 --batchingest.bench.*= 覆盖。
 * 启动时 --batchingest.bench.enabled=true 则立即跑压测、打印结果后退出。
 */
@ConfigurationProperties(prefix = "batchingest.bench")
public class BenchProperties {

    /** 是否启用压测模式（启动即压力测试，跑完退出） */
    private boolean enabled = false;

    /** 实现模式：adaptive | static | legacy */
    private String mode = "adaptive";

    /** 压测线程数（汇聚端并发注入） */
    private int threads = 8;

    /** 目标汇聚速率（条/秒）；flood=true 时忽略 */
    private int rate = 5000;

    /** 压测持续秒数 */
    private int durationSec = 15;

    /** 满速压测（忽略 rate，以最大速率汇聚） */
    private boolean flood = false;

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

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(int durationSec) {
        this.durationSec = durationSec;
    }

    public boolean isFlood() {
        return flood;
    }

    public void setFlood(boolean flood) {
        this.flood = flood;
    }
}