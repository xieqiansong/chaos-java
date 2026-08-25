package lan.chaos.batchwriter.writer;

import java.util.List;

/**
 * 批量入库 Writer 统一抽象：汇聚入口 + 指标统计 + 生命周期。
 *
 * <p>三个实现：
 * <ul>
 *   <li>{@code legacy}：每调用一次即发一次 Redis 写命令（基准，无批量）；</li>
 *   <li>{@code static}：定批 + Pipeline（有批量但批量大小固定）；</li>
 *   <li>{@code adaptive}：自适应批量引擎（目标方案，批量大小在线寻优）。</li>
 * </ul>
 */
public interface BatchWriter<T> {

    /** 实现名称 */
    String name();

    /** 汇聚入口：把一个 item 交给 writer（内部攒批或直写） */
    void write(T item);

    /** 实际写入 Redis 的条目数 */
    long itemsWritten();

    /** Redis 往返次数（命令批次）。legacy = 每条命令；批量实现 = 每批一次 Pipeline */
    long redisCalls();

    /** 平均批量大小（itemsWritten / 命令批次），legacy 恒为 1 */
    double avgBatchSize();

    /** Redis 写入失败计数（连接抖动/超时导致的写入失败条数） */
    default long errors() {
        return 0;
    }

    /** 队列满导致的丢弃条数（批量实现；legacy 无队列恒为 0） */
    default long dropped() {
        return 0;
    }

    /** 启动（批量实现需启动消费线程） */
    void start();

    /** 停止并刷出剩余数据 */
    void close();
}