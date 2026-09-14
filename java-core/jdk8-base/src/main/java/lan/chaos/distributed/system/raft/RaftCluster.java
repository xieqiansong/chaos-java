package lan.chaos.distributed.system.raft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Raft 内存版集群模拟器：负责节点间的消息投递、选举超时驱动与心跳。
 *
 * <p>WHY：Raft 论文只定义了节点间的 RPC 语义，真正的「谁在什么时候给谁发消息」由运行环境决定。
 * 本类扮演这个运行环境——用内存 mailbox 代替网络，用逻辑 tick 代替真实时钟，把论文里的
 * RequestVote / AppendEntries 跑成一段可复现、可断言的剧本。
 *
 * <p>关键点：节点初始选举超时做了<b>错位</b>（node i 的超时 = base + i*stagger），
 * 保证只有一个节点最先超时发起选举、先拿到多数票上位，避免真实场景里「同时竞选→分裂投票→反复重选」，
 * 同时在本 demo 里保持确定性（测试不 flaky）。生产环境必须用随机超时。
 */
public class RaftCluster {

    private final List<RaftNode> nodes = new ArrayList<>();
    private final int quorum;
    private final int base;
    private final Map<Integer, Queue<RaftNode.RaftMessage>> mailbox = new HashMap<>();

    /**
     * @param size 集群节点数（建议奇数，3/5 最常用）
     * @param base 选举超时基值（逻辑 tick 数）；节点间在此基础上错位
     */
    public RaftCluster(int size, int base) {
        if (size < 1) throw new IllegalArgumentException("集群节点数至少为 1");
        this.base = base;
        this.quorum = size / 2 + 1;
        int stagger = Math.max(1, base / 2 + 1);
        for (int i = 0; i < size; i++) {
            RaftNode n = new RaftNode(i, size, base + i * stagger);
            nodes.add(n);
            mailbox.put(i, new ArrayDeque<>());
        }
    }

    private RaftNode node(int id) { return nodes.get(id); }

    public List<RaftNode> getNodes() { return new ArrayList<>(nodes); }

    public RaftNode getLeader() {
        RaftNode leader = null;
        for (RaftNode n : nodes) {
            if (n.getRole() == RaftNode.Role.LEADER) {
                if (leader != null) return null; // 出现两个 leader 视为异常
                leader = n;
            }
        }
        return leader;
    }

    /**
     * 跑选举，直到出现稳定 leader（连续若干 tick 唯一且不变）。
     *
     * @param maxTicks 安全上限，防止极端情况下死循环
     * @return 选出的 leader；超时仍未选出则返回当前 leader（可能为 null）
     */
    public RaftNode electLeader(int maxTicks) {
        int stable = 0;
        for (int tick = 0; tick < maxTicks; tick++) {
            // 1) 触发选举超时：最先超时的节点发起竞选
            for (RaftNode n : nodes) {
                if (n.isElectionTimeout()) {
                    n.startElection();
                    RaftNode.RequestVote rv = n.makeRequestVote();
                    for (RaftNode o : nodes) {
                        if (o.id != n.id) deliver(o.id, rv);
                    }
                    n.resetElectionTimer();
                }
            }
            drain();

            // 2) leader 发心跳，压制 follower 的选举超时
            RaftNode leader = getLeader();
            if (leader != null) {
                for (RaftNode o : nodes) {
                    if (o.id != leader.id) deliver(o.id, leader.makeAppendEntries(o.id));
                }
                drain();
            }

            // 3) 逻辑时钟推进
            for (RaftNode n : nodes) n.tick();

            // 4) 稳定判定：唯一 leader 连续存活若干 tick 即视为选举完成
            if (getLeader() != null) {
                stable++;
                if (stable >= 3) return getLeader();
            } else {
                stable = 0;
            }
        }
        return getLeader();
    }

    /**
     * 由当前 leader 复制一条命令：追加到本地日志 → 扩散到 follower → 多数派确认后提交 → 应用到状态机。
     *
     * @return 提交的日志下标（未选出 leader 时抛异常）
     */
    public int replicate(String command) {
        RaftNode leader = getLeader();
        if (leader == null) throw new IllegalStateException("当前无 leader，无法复制日志");
        int index = leader.appendCommand(command);
        leader.tryCommit(); // 单节点等自身即多数派时立即提交
        int maxRounds = nodes.size() * 4 + 4;
        for (int round = 0; round < maxRounds; round++) {
            for (RaftNode o : nodes) {
                if (o.id != leader.id) deliver(o.id, leader.makeAppendEntries(o.id));
            }
            drain();
            // 继续推进，直到 leader 已提交且所有 follower 也都把该下标应用到状态机
            // （关键：leader 自身提交后必须再用一次 AppendEntries 把 commitIndex 广播给 follower）
            boolean allApplied = leader.commitIndex() >= index;
            for (RaftNode o : nodes) {
                if (o.id != leader.id) allApplied &= (o.commitIndex() >= index);
            }
            if (allApplied) break;
        }
        return index;
    }

    // ===================== 传输层（内存 mailbox） =====================

    private void deliver(int toId, RaftNode.RaftMessage msg) {
        msg.to = toId;
        mailbox.get(toId).add(msg);
    }

    /** 反复排空所有 mailbox，直到没有待处理消息（消息处理中可能继续产生回复）。 */
    private void drain() {
        int guard = 10000;
        while (hasPending() && guard-- > 0) {
            for (RaftNode n : nodes) {
                Queue<RaftNode.RaftMessage> q = mailbox.get(n.id);
                while (!q.isEmpty()) {
                    RaftNode.RaftMessage m = q.poll();
                    if (m instanceof RaftNode.RequestVote) {
                        RaftNode.RequestVote rv = (RaftNode.RequestVote) m;
                        RaftNode.RequestVoteResponse r = n.handleRequestVote(rv);
                        r.to = rv.from; // 回给候选人
                        deliver(rv.from, r);
                    } else if (m instanceof RaftNode.RequestVoteResponse) {
                        n.onRequestVoteResponse((RaftNode.RequestVoteResponse) m);
                    } else if (m instanceof RaftNode.AppendEntries) {
                        RaftNode.AppendEntries ae = (RaftNode.AppendEntries) m;
                        RaftNode.AppendEntriesResponse r = n.handleAppendEntries(ae);
                        r.to = ae.from; // 回给 leader
                        deliver(ae.from, r);
                    } else if (m instanceof RaftNode.AppendEntriesResponse) {
                        // 消息当前在 leader 的 mailbox 中，n 即 leader；回应方是 follower（m.from）
                        n.onAppendEntriesResponse(m.from, (RaftNode.AppendEntriesResponse) m);
                    }
                }
            }
        }
    }

    private boolean hasPending() {
        for (Queue<RaftNode.RaftMessage> q : mailbox.values()) {
            if (!q.isEmpty()) return true;
        }
        return false;
    }
}
