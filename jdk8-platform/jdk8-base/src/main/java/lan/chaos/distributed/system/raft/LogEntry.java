package lan.chaos.distributed.system.raft;

/**
 * Raft 日志条目（Raft 论文 Figure 2 中的 log[]）。
 *
 * <p>每条日志都由「任期 + 命令」组成；<b>贯穿整个集群的日志索引是全局一致的位置标尺</b>，
 * 复制与提交都以 (term, index) 为凭据。命令在本 demo 中简化为 {@code key=value} 形式的 KV 写入。
 */
public class LogEntry {

    /** 日志在节点本地数组中的下标，从 0 开始连续递增。 */
    public final int index;
    /** 该条目被 leader 创建时所在的任期；用于日志匹配与提交约束。 */
    public final long term;
    /** 被状态机执行的命令；本 demo 约定为 {@code key=value}。 */
    public final String command;

    public LogEntry(int index, long term, String command) {
        this.index = index;
        this.term = term;
        this.command = command;
    }

    @Override
    public String toString() {
        return "LogEntry{index=" + index + ", term=" + term + ", command='" + command + "'}";
    }
}
