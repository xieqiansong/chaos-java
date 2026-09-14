package lan.chaos.distributed.system.raft;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Raft 共识模拟的可断言测试（JUnit 5）：验证选举、日志复制、状态机一致性等关键语义。
 * 由于模拟是确定性、无真实线程/网络的，测试稳定可复现。
 */
class RaftConsensusTest {

    @Test
    void election_picksExactlyOneLeader() {
        RaftCluster cluster = new RaftCluster(5, 10);
        RaftNode leader = cluster.electLeader(500);

        assertNotNull(leader, "应当选出 leader");
        assertEquals(RaftNode.Role.LEADER, leader.getRole());
        assertEquals(1, cluster.getNodes().stream()
                .map(RaftNode::getRole)
                .filter(r -> r == RaftNode.Role.LEADER)
                .count(), "同一时刻只能有一个 leader");
        assertTrue(leader.currentTerm() >= 1, "任期应从 1 开始累计");
    }

    @Test
    void singleNodeCluster_isImmediateLeader() {
        RaftCluster cluster = new RaftCluster(1, 10);
        RaftNode leader = cluster.electLeader(50);

        assertNotNull(leader);
        assertEquals(0, leader.id);
        assertEquals(RaftNode.Role.LEADER, leader.getRole());
    }

    @Test
    void replication_commitsAndAppliesToStateMachine() {
        RaftCluster cluster = new RaftCluster(3, 10);
        RaftNode leader = cluster.electLeader(500);
        assertNotNull(leader);

        int idx = cluster.replicate("k1=v1");

        assertEquals(0, idx, "首条日志下标应为 0");
        assertEquals(0, leader.commitIndex(), "leader 应已提交该日志");
        for (RaftNode n : cluster.getNodes()) {
            Map<String, String> sm = n.getStateMachine();
            assertEquals(1, sm.size(), "每个节点状态机应写入 1 条");
            assertEquals("v1", sm.get("k1"));
            assertEquals(0, n.commitIndex());
        }
    }

    @Test
    void replication_allNodesConverge() {
        RaftCluster cluster = new RaftCluster(5, 10);
        RaftNode leader = cluster.electLeader(500);
        assertNotNull(leader);

        cluster.replicate("user:1=Alice");
        cluster.replicate("user:2=Bob");
        cluster.replicate("user:3=Carol");

        Map<String, String> expected = leader.getStateMachine();
        assertEquals(3, expected.size());

        for (RaftNode n : cluster.getNodes()) {
            assertEquals(expected, n.getStateMachine(),
                    "node-" + n.id + " 的状态机应与 leader 完全一致（共识）");
            assertEquals(2, n.commitIndex(), "所有节点应已提交到下标 2");
        }
    }

    @Test
    void leaderMustExistBeforeReplicate() {
        // 未选举时直接复制应抛异常
        RaftCluster cluster = new RaftCluster(3, 10);
        assertThrows(IllegalStateException.class, () -> cluster.replicate("x=1"));
    }

    @Test
    void logLengthMatchesReplicatedCommands() {
        RaftCluster cluster = new RaftCluster(3, 10);
        RaftNode leader = cluster.electLeader(500);
        cluster.replicate("a=1");
        cluster.replicate("b=2");

        List<LogEntry> log = leader.getLog();
        assertEquals(2, log.size());
        assertEquals(0, log.get(0).index);
        assertEquals(1, log.get(1).index);
        assertEquals("a=1", log.get(0).command);
    }
}
