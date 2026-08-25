package lan.chaos.batchwriter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 批量入库引擎配置（前缀 batchingest）。命令行 --batchingest.*= 可覆盖。
 */
@ConfigurationProperties(prefix = "batchingest")
public class BatchWriterProperties {

    /** 默认实现：adaptive | static | legacy */
    private String mode = "adaptive";

    /** Redis 写入 Hash 的 key 前缀 */
    private String keyPrefix = "batch:demo";

    /** 内存桶（ArrayBlockingQueue）容量上限 */
    private int queueCapacity = 2_000_000;

    /** 加速线程启用/停用的临界水位 */
    private int queueCritical = 200_000;

    /** 初始批量大小 */
    private int batchInitial = 2048;

    /** 批量收敛上界 */
    private int batchMin = 128;

    /** 批量收敛上界 */
    private int batchMax = 20_480;

    /** 探索概率（满量 flush 时随机试探候选批量） */
    private double exploreProb = 0.2;

    /** 静默兜底 flush 间隔（毫秒） */
    private long idleFlushMs = 8000;

    /** 速度反馈指数衰减因子（按秒） */
    private double decayFactor = 0.9;

    /** 平滑过渡：当前值权重（新值 = 当前×smooth + 最优×(1-smooth)） */
    private double smoothCurrentWeight = 0.3;

    /** 每 N 次 flush 调整一次批量大小 */
    private int sampleWindow = 100;

    /** 是否启用加速线程（突发削峰） */
    private boolean useAccelerator = true;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getQueueCritical() {
        return queueCritical;
    }

    public void setQueueCritical(int queueCritical) {
        this.queueCritical = queueCritical;
    }

    public int getBatchInitial() {
        return batchInitial;
    }

    public void setBatchInitial(int batchInitial) {
        this.batchInitial = batchInitial;
    }

    public int getBatchMin() {
        return batchMin;
    }

    public void setBatchMin(int batchMin) {
        this.batchMin = batchMin;
    }

    public int getBatchMax() {
        return batchMax;
    }

    public void setBatchMax(int batchMax) {
        this.batchMax = batchMax;
    }

    public double getExploreProb() {
        return exploreProb;
    }

    public void setExploreProb(double exploreProb) {
        this.exploreProb = exploreProb;
    }

    public long getIdleFlushMs() {
        return idleFlushMs;
    }

    public void setIdleFlushMs(long idleFlushMs) {
        this.idleFlushMs = idleFlushMs;
    }

    public double getDecayFactor() {
        return decayFactor;
    }

    public void setDecayFactor(double decayFactor) {
        this.decayFactor = decayFactor;
    }

    public double getSmoothCurrentWeight() {
        return smoothCurrentWeight;
    }

    public void setSmoothCurrentWeight(double smoothCurrentWeight) {
        this.smoothCurrentWeight = smoothCurrentWeight;
    }

    public int getSampleWindow() {
        return sampleWindow;
    }

    public void setSampleWindow(int sampleWindow) {
        this.sampleWindow = sampleWindow;
    }

    public boolean isUseAccelerator() {
        return useAccelerator;
    }

    public void setUseAccelerator(boolean useAccelerator) {
        this.useAccelerator = useAccelerator;
    }
}