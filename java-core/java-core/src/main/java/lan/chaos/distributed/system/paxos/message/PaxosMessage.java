package lan.chaos.distributed.system.paxos.message;
// ==================== 消息定义 ====================

public abstract class PaxosMessage {
    protected final int from;
    protected final int to;

    public PaxosMessage(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public int getFrom() { return from; }
    public int getTo() { return to; }
}
