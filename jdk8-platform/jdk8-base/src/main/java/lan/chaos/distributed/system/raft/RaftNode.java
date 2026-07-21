package lan.chaos.distributed.system.raft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raft 共识算法核心节点（单线程、内存态、确定性模拟）。
 *
 * <p>WHY：分布式系统最难的不是「算」，而是「让一堆会宕机、会网络延迟的节点对一件事达成一致」。
 * Raft 把共识拆成三个子问题，比 Paxos 好懂得多：
 * <ul>
 *   <li><b>Leader 选举</b>：节点靠「任期 + 超时」选出唯一 leader，所有写请求都走 leader；</li>
 *   <li><b>日志复制</b>：leader 把命令作为日志追加，复制到多数派后提交，再应用到状态机；</li>
 *   <li><b>安全性</b>：选举限制（候选人的日志必须至少和投票人一样新）+ 只提交当前任期的日志，保证不丢、不重、不乱序。</li>
 * </ul>
 *
 * <p>本实现刻意做成<b>确定性、无真实线程/网络</b>的模拟：节点之间靠 {@link RaftCluster} 在内存里投递消息，
 * 选举超时用「逻辑倒计时」驱动。好处是测试可断言、可复现，不会被真实调度抖动的随机性搞成 flaky test。
 *
 * <p>关键状态（对应论文 Figure 2）：
 * <ul>
 *   <li>持久化：currentTerm（当前任期）、votedFor（本任期投了谁）、log（日志）；</li>
 *   <li>易失：commitIndex（已提交最高下标）、lastApplied（已应用最高下标）、role、选举倒计时；</li>
 *   <li>leader 专用：nextIndex[] / matchIndex[]（每个 follower 的复制进度）。</li>
 * </ul>
 *
 * <p>生产坑：真实 Raft 里选举超时必须是<b>随机</b>的（避免同时竞选导致反复分裂投票）；
 * 本 demo 用「错位固定超时」达到同样效果且可复现，生产务必随机化。
 */
public class RaftNode {

    /** 节点角色。 */
    public enum Role { FOLLOWER, CANDIDATE, LEADER }

    // ============ 持久化状态（崩溃后必须能恢复） ============
    private long currentTerm = 0;
    private int votedFor = -1;                 // 本任期把票投给了谁；-1 表示尚未投票
    private final List<LogEntry> log = new ArrayList<>();

    // ============ 易失状态 ============
    private Role role = Role.FOLLOWER;
    private int commitIndex = -1;              // 已知已提交的最高日志下标
    private int lastApplied = -1;              // 已应用到状态机的最高日志下标
    private int electionTimer;                 // 逻辑选举倒计时，归零触发选举
    private int votes;                         // 本届选举累计收到的赞成票（仅 candidate 使用）
    private final int electionTimeoutBase;     // 选举超时基值；收到合法 leader 心跳时重置为该值

    // leader 专用：每个节点的复制进度
    private final int[] nextIndex;             // 下一条要发给该 follower 的日志下标
    private final int[] matchIndex;            // 该 follower 已复制的最高日志下标（初始 -1）

    // 状态机：本 demo 用 LinkedHashMap 模拟 KV 存储，apply 已提交日志
    private final Map<String, String> stateMachine = new LinkedHashMap<>();

    public final int id;
    public final int clusterSize;

    public RaftNode(int id, int clusterSize, int electionTimeout) {
        this.id = id;
        this.clusterSize = clusterSize;
        this.electionTimeoutBase = electionTimeout;
        this.electionTimer = electionTimeout;
        this.nextIndex = new int[clusterSize];
        this.matchIndex = new int[clusterSize];
    }

    // ===================== 通用辅助 =====================

    public Role getRole() { return role; }
    public long currentTerm() { return currentTerm; }
    public int commitIndex() { return commitIndex; }
    public int lastApplied() { return lastApplied; }
    public int lastLogIndex() { return log.size() - 1; }
    public long lastLogTerm() { return log.isEmpty() ? 0L : log.get(log.size() - 1).term; }
    public int quorum() { return clusterSize / 2 + 1; }
    public List<LogEntry> getLog() { return new ArrayList<>(log); }
    public Map<String, String> getStateMachine() { return new LinkedHashMap<>(stateMachine); }

    /** 逻辑时钟每 tick 减一；leader 不会触发选举。 */
    void tick() { if (electionTimer > 0) electionTimer--; }
    boolean isElectionTimeout() { return role != Role.LEADER && electionTimer <= 0; }
    void resetElectionTimer() { electionTimer = electionTimeoutBase; }

    // ===================== 选举：RequestVote =====================

    /** 候选人发起投票：自增任期、投自己一票、转为 candidate。 */
    void startElection() {
        currentTerm++;
        role = Role.CANDIDATE;
        votedFor = id;
        votes = 1;
        resetElectionTimer();
        // 单节点集群：自身即多数派，无需等待任何回复即可直接上位
        if (votes >= quorum()) becomeLeader();
    }

    /** 构造发往指定 follower 的投票请求（from/term/日志进度）。to 由 Cluster 填充。 */
    RequestVote makeRequestVote() {
        return new RequestVote(id, -1, currentTerm, lastLogIndex(), lastLogTerm());
    }

    /**
     * 处理投票请求。
     * 关键约束：只有「任期不旧、且日志至少和投票人一样新」的候选人才拿得到票——
     * 这就是 Raft 安全性里「只让数据最全的节点当 leader」的核心。
     */
    RequestVoteResponse handleRequestVote(RequestVote rv) {
        if (rv.term > currentTerm) {           // 见到更大任期，立即认怂退为 follower
            currentTerm = rv.term;
            role = Role.FOLLOWER;
            votedFor = -1;
        }
        boolean grant = false;
        if (rv.term == currentTerm) {
            boolean logUpToDate = (rv.lastLogTerm > lastLogTerm())
                    || (rv.lastLogTerm == lastLogTerm() && rv.lastLogIndex >= lastLogIndex());
            if ((votedFor == -1 || votedFor == rv.from) && logUpToDate) {
                votedFor = rv.from;
                grant = true;
            }
        }
        return new RequestVoteResponse(id, rv.from, currentTerm, grant);
    }

    /** 收到投票回复：凑够多数派就上位成 leader。 */
    void onRequestVoteResponse(RequestVoteResponse resp) {
        if (role != Role.CANDIDATE) return;
        if (resp.term > currentTerm) {         // 别人任期更大，退位
            currentTerm = resp.term;
            role = Role.FOLLOWER;
            votedFor = -1;
            return;
        }
        if (resp.voteGranted) {
            votes++;
            if (votes >= quorum()) becomeLeader();
        }
    }

    /** 当选 leader：初始化复制进度（follower 的 nextIndex 指向自己日志末尾）。 */
    private void becomeLeader() {
        role = Role.LEADER;
        for (int i = 0; i < clusterSize; i++) {
            nextIndex[i] = lastLogIndex() + 1;
            matchIndex[i] = -1;
        }
        matchIndex[id] = lastLogIndex();       // leader 自己天然拥有全部日志
    }

    // ===================== 复制：AppendEntries =====================

    /** leader 为本 follower 构造追加日志请求（含心跳：entries 为空即心跳）。 */
    AppendEntries makeAppendEntries(int followerId) {
        int ni = nextIndex[followerId];
        int prevLogIndex = ni - 1;
        long prevLogTerm = prevLogIndex >= 0 ? log.get(prevLogIndex).term : 0L;
        // 拷贝切片，避免与本地 log 共享可变引用
        List<LogEntry> entries = new ArrayList<>(log.subList(ni, log.size()));
        return new AppendEntries(id, followerId, currentTerm, prevLogIndex, prevLogTerm, entries, commitIndex);
    }

    /** leader 追加一条命令到本地日志，返回其下标。 */
    int appendCommand(String command) {
        int idx = log.size();
        log.add(new LogEntry(idx, currentTerm, command));
        return idx;
    }

    /** 本地追加后由集群调用：尝试推进提交（leader 自计一票，单节点集群可立即提交）。 */
    void tryCommit() { maybeAdvanceCommit(); }

    /**
     * 处理追加日志（同时也是心跳）。
     * 一致性检查：prevLogIndex/prevLogTerm 必须和本地日志吻合，否则拒绝（leader 会回退 nextIndex 重试）。
     */
    AppendEntriesResponse handleAppendEntries(AppendEntries ae) {
        if (ae.term < currentTerm) {
            return new AppendEntriesResponse(id, ae.from, currentTerm, false, -1);
        }
        currentTerm = Math.max(currentTerm, ae.term);
        role = Role.FOLLOWER;                  // 合法 leader 出现，任何 candidate 都得退位
        votedFor = -1;
        resetElectionTimer();                  // 收到心跳则重置选举倒计时，避免抢班夺权

        // 日志匹配检查
        if (ae.prevLogIndex >= 0) {
            if (log.size() <= ae.prevLogIndex) {
                return new AppendEntriesResponse(id, ae.from, currentTerm, false, -1);
            }
            if (log.get(ae.prevLogIndex).term != ae.prevLogTerm) {
                return new AppendEntriesResponse(id, ae.from, currentTerm, false, -1);
            }
        }

        // 追加/覆盖冲突条目
        int insertAt = ae.prevLogIndex + 1;
        for (int i = 0; i < ae.entries.size(); i++) {
            LogEntry e = ae.entries.get(i);
            int pos = insertAt + i;
            if (pos < log.size()) {
                if (log.get(pos).term != e.term) {     // 冲突：截断其后全部，再追加
                    log.subList(pos, log.size()).clear();
                    log.add(e);
                }
            } else {
                log.add(e);
            }
        }

        // 推进提交下标，并应用到状态机
        if (ae.leaderCommit > commitIndex) {
            commitIndex = Math.min(ae.leaderCommit, log.size() - 1);
        }
        applyCommitted();

        int match = ae.prevLogIndex + ae.entries.size();
        return new AppendEntriesResponse(id, ae.from, currentTerm, true, match);
    }

    /** leader 收到 follower 的复制确认：更新进度并推进提交。 */
    void onAppendEntriesResponse(int followerId, AppendEntriesResponse resp) {
        if (role != Role.LEADER) return;
        if (resp.term > currentTerm) {
            currentTerm = resp.term;
            role = Role.FOLLOWER;
            votedFor = -1;
            return;
        }
        if (!resp.success) {
            nextIndex[followerId] = Math.max(0, nextIndex[followerId] - 1); // 回退重试
            return;
        }
        matchIndex[followerId] = resp.matchIndex;
        maybeAdvanceCommit();
    }

    /**
     * 尝试推进 commitIndex：某个下标被<b>多数派</b>复制、且属于<b>当前任期</b>时才能提交。
     * 只提交当前任期日志是 Raft 的关键安全约束——防止「旧任期的日志被间接提交后又被新 leader 覆盖」。
     */
    private void maybeAdvanceCommit() {
        for (int n = commitIndex + 1; n < log.size(); n++) {
            if (log.get(n).term != currentTerm) continue;
            int count = 1; // leader 自己
            for (int i = 0; i < clusterSize; i++) {
                if (i != id && matchIndex[i] >= n) count++;
            }
            if (count >= quorum()) {
                commitIndex = n;
            } else {
                break;
            }
        }
        applyCommitted();
    }

    /** 把已提交但尚未应用的日志顺序应用到状态机（KV 写入）。 */
    private void applyCommitted() {
        while (lastApplied < commitIndex) {
            lastApplied++;
            applyCommand(log.get(lastApplied).command);
        }
    }

    private void applyCommand(String command) {
        int eq = command.indexOf('=');
        if (eq < 0) {
            stateMachine.put(command, "");
        } else {
            stateMachine.put(command.substring(0, eq), command.substring(eq + 1));
        }
    }

    // ===================== 消息类型（内存投递用，携带 from/to 路由信息） =====================

    abstract static class RaftMessage {
        int from;
        int to;
        RaftMessage(int from, int to) { this.from = from; this.to = to; }
    }

    static class RequestVote extends RaftMessage {
        final long term;
        final int lastLogIndex;
        final long lastLogTerm;
        RequestVote(int from, int to, long term, int lastLogIndex, long lastLogTerm) {
            super(from, to); this.term = term; this.lastLogIndex = lastLogIndex; this.lastLogTerm = lastLogTerm;
        }
    }

    static class RequestVoteResponse extends RaftMessage {
        final long term;
        final boolean voteGranted;
        RequestVoteResponse(int from, int to, long term, boolean voteGranted) {
            super(from, to); this.term = term; this.voteGranted = voteGranted;
        }
    }

    static class AppendEntries extends RaftMessage {
        final long term;
        final int prevLogIndex;
        final long prevLogTerm;
        final List<LogEntry> entries;
        final int leaderCommit;
        AppendEntries(int from, int to, long term, int prevLogIndex, long prevLogTerm,
                      List<LogEntry> entries, int leaderCommit) {
            super(from, to);
            this.term = term; this.prevLogIndex = prevLogIndex; this.prevLogTerm = prevLogTerm;
            this.entries = entries; this.leaderCommit = leaderCommit;
        }
    }

    static class AppendEntriesResponse extends RaftMessage {
        final long term;
        final boolean success;
        final int matchIndex;
        AppendEntriesResponse(int from, int to, long term, boolean success, int matchIndex) {
            super(from, to); this.term = term; this.success = success; this.matchIndex = matchIndex;
        }
    }
}
