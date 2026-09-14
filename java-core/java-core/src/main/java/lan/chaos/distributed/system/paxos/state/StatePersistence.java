package lan.chaos.distributed.system.paxos.state;
// ==================== 持久化抽象 ====================

public interface StatePersistence {
    /** 加载持久化的 Acceptor 状态，无数据时返回默认值 */
    AcceptorState load();

    /** 持久化完整的 Acceptor 状态（同步刷盘） */
    void save(AcceptorState state);

    /** 加载 Proposer 当前轮次，用于生成提案编号 */
    long loadCurrentRound();

    /** 持久化 Proposer 当前轮次 */
    void saveCurrentRound(long round);
}
