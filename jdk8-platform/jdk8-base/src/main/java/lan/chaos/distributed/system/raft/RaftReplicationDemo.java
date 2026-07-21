package lan.chaos.distributed.system.raft;
// ==================== 场景二：日志复制 + 状态机一致性 ====================

/**
 * Raft 日志复制演示：leader 把多条 KV 命令作为日志追加，复制到多数派后提交，
 * 各节点把已提交日志应用到状态机，最终所有节点的状态机完全一致——这就是「共识」的落地形态。
 *
 * <p>运行：直接 {@code main}。重点观察：无论命令写在 leader 上，follower 的状态机最终与 leader 完全相同。
 */
public class RaftReplicationDemo {
    public static void main(String[] args) {
        int clusterSize = 3;
        RaftCluster cluster = new RaftCluster(clusterSize, 10);

        RaftNode leader = cluster.electLeader(500);
        System.out.println("===== Raft 日志复制演示（集群规模=" + clusterSize + "）=====");
        System.out.println("Leader = node-" + leader.id + " (term=" + leader.currentTerm() + ")");
        System.out.println("客户端向 leader 顺序写入 3 条命令...\n");

        String[] commands = {"user:1=Alice", "user:2=Bob", "user:3=Carol"};
        for (String cmd : commands) {
            int idx = cluster.replicate(cmd);
            System.out.println("  写入 [" + cmd + "] => 提交下标 index=" + idx);
        }

        System.out.println("\n各节点状态机（应完全一致，体现共识）：");
        for (RaftNode n : cluster.getNodes()) {
            System.out.println("  node-" + n.id + " (" + n.getRole() + ") commitIndex="
                    + n.commitIndex() + " state=" + n.getStateMachine());
        }
    }
}
