package lan.chaos.distributed.system.paxos.message;

// ==================== 基础类型 ====================

/**
 * 全局唯一递增的提案编号，由轮次和节点ID组合
 */
public class ProposalId implements Comparable<ProposalId> {
    private final long round;
    private final int nodeId;

    public ProposalId(long round, int nodeId) {
        this.round = round;
        this.nodeId = nodeId;
    }

    public long toLong() {
        return (round << 32) | (nodeId & 0xFFFFFFFFL);
    }

    public static ProposalId fromLong(long value) {
        return new ProposalId(value >>> 32, (int) value);
    }

    public ProposalId nextRound() {
        return new ProposalId(round + 1, nodeId);
    }

    @Override
    public int compareTo(ProposalId other) {
        return Long.compare(this.toLong(), other.toLong());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProposalId)) return false;
        return compareTo((ProposalId) o) == 0;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(toLong());
    }

    @Override
    public String toString() {
        return "(" + round + "," + nodeId + ")";
    }
}
