package lan.chaos.rocketmq.common.idempotent;

/**
 * 消费幂等去重存储抽象。
 * <p>
 * 生产环境应替换为分布式存储（如 Redis {@code SETNX + EX}、或数据库唯一键），
 * 保证多实例部署、进程重启后去重依然有效。当前内存实现仅用于演示原理。
 */
public interface MessageIdStore {

    /** 该消息是否已经处理过（用于跳过重复消费） */
    boolean isProcessed(String msgId);

    /** 标记消息已处理 */
    void markProcessed(String msgId);
}
