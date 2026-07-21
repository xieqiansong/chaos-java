package lan.chaos.distributed.system.raft;
// ==================== 场景一：Leader 选举 ====================

/**
 * Raft Leader 选举演示：节点靠「任期 + 超时」选出唯一 leader，集群中所有写请求都走 leader。
 *
 * <p>运行：直接 {@code main}。重点观察：
 * <ul>
 *   <li>集群初始全是 FOLLOWER，靠<b>错位</b>的选举超时保证只有一个节点最先发起竞选；</li>
 *   <li>候选人自增任期、投自己一票、向其他节点拉票，拿到多数派（quorum）即上位；</li>
 *   <li>选出的 leader 立即发心跳（AppendEntries）压制其余节点的选举超时，维持稳定；</li>
 *   <li>同一时刻集群中<b>恰好一个</b> leader——与真实 ZooKeeper 选主、Redis 哨兵同理。</li>
 * </ul>
 *
 * <p>与 {@link RaftReplicationDemo}（场景二：日志复制）配合，才能看到 Raft 的完整形态：
 * 先选举出 leader，再由 leader 把客户端命令复制成日志并达成共识。</p>
 */
public class RaftElectionDemo {
    public static void main(String[] args) {
        int clusterSize = 5;
        RaftCluster cluster = new RaftCluster(clusterSize, 10);

        System.out.println("===== Raft Leader 选举演示（集群规模=" + clusterSize + "）=====");
        System.out.println("初始：所有节点都是 FOLLOWER，选举计时器错位启动\n");

        RaftNode leader = cluster.electLeader(500);

        System.out.println("选举完成：");
        System.out.println("  Leader = node-" + leader.id + " (term=" + leader.currentTerm() + ")");
        System.out.println("  各节点角色：");
        for (RaftNode n : cluster.getNodes()) {
            System.out.println("    node-" + n.id + " -> " + n.getRole() + " (term=" + n.currentTerm() + ")");
        }

        long leaders = cluster.getNodes().stream()
                .filter(n -> n.getRole() == RaftNode.Role.LEADER)
                .count();
        System.out.println("\n断言性观察：集群中 Leader 数量 = " + leaders + "（共识要求恒为 1）");
    }
}
