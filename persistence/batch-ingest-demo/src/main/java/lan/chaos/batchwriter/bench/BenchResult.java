package lan.chaos.batchwriter.bench;

/**
 * 一次压测的结构化结果。itemsPerSec / redisPerSec 均为约每秒，通过真实耗时计算。
 */
public class BenchResult {

    /** 实现模式 */
    public final String mode;
    /** 实际写入 Redis 的条目数 */
    public final long totalWritten;
    /** 每秒写入条目 */
    public final double itemsPerSec;
    /** Redis 往返命令数 */
    public final long redisCalls;
    /** 每秒 Redis 往返（=每秒命令批次） */
    public final double redisPerSec;
    /** 平均批量大小（legacy 恒 1） */
    public final double avgBatchSize;
    /** 队列满丢弃数（adaptive） */
    public final long dropped;
    /** Redis 写入失败条数（连接抖动/超时） */
    public final long errors;

    public BenchResult(String mode, long totalWritten, double itemsPerSec,
                       long redisCalls, double redisPerSec, double avgBatchSize, long dropped, long errors) {
        this.mode = mode;
        this.totalWritten = totalWritten;
        this.itemsPerSec = itemsPerSec;
        this.redisCalls = redisCalls;
        this.redisPerSec = redisPerSec;
        this.avgBatchSize = avgBatchSize;
        this.dropped = dropped;
        this.errors = errors;
    }
}