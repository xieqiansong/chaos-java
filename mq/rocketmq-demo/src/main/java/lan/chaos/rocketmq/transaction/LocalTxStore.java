package lan.chaos.rocketmq.transaction;

/**
 * 本地事务状态存储抽象。
 * <p>
 * 事务消息回查（{@code checkLocalTransaction}）时，Broker 会反复回调询问本地事务最终结果。
 * 正确做法是"查库"判定（如查询订单表状态），而不是依赖易失的内存变量。
 * 生产环境用数据库订单状态表替换本内存实现。
 */
public interface LocalTxStore {

    /** 记录本地事务执行结果（true=已提交，false=已回滚） */
    void mark(String bizKey, boolean committed);

    /** 查询本地事务最终状态；null 表示状态未知（回查时应返回 UNKNOWN） */
    Boolean get(String bizKey);
}
