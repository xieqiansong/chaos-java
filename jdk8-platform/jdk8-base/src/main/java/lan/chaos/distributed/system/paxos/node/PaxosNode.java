package lan.chaos.distributed.system.paxos.node;
// ==================== 整合节点 ====================

import lan.chaos.distributed.system.paxos.message.AcceptRequest;
import lan.chaos.distributed.system.paxos.message.AcceptResponse;
import lan.chaos.distributed.system.paxos.message.ChosenNotification;
import lan.chaos.distributed.system.paxos.message.PrepareRequest;
import lan.chaos.distributed.system.paxos.message.PrepareResponse;
import lan.chaos.distributed.system.paxos.role.Acceptor;
import lan.chaos.distributed.system.paxos.role.Learner;
import lan.chaos.distributed.system.paxos.role.Proposer;
import lan.chaos.distributed.system.paxos.state.InMemoryStatePersistence;
import lan.chaos.distributed.system.paxos.state.StatePersistence;
import lan.chaos.distributed.system.paxos.transport.LocalMessenger;
import lan.chaos.distributed.system.paxos.transport.Messenger;

public class PaxosNode {
    private final int nodeId;
    private final Proposer proposer;
    private final Acceptor acceptor;
    private final Learner learner;
    private final Messenger messenger;

    public PaxosNode(int nodeId, int clusterSize, long timeoutMs) {
        this.nodeId = nodeId;
        StatePersistence persistence = new InMemoryStatePersistence(); // 可替换为文件/RocksDB
        LocalMessenger localMessenger = new LocalMessenger();
        this.messenger = localMessenger;
        this.proposer = new Proposer(nodeId, persistence, messenger, clusterSize, timeoutMs);
        this.acceptor = new Acceptor(nodeId, persistence, messenger);
        this.learner = new Learner(nodeId);
        // 注册消息处理器
        localMessenger.registerNode(nodeId, message -> {
            if (message instanceof PrepareRequest || message instanceof AcceptRequest) {
                acceptor.onMessage(message);
            } else if (message instanceof PrepareResponse || message instanceof AcceptResponse) {
                proposer.onMessage(message);
            } else if (message instanceof ChosenNotification) {
                learner.onMessage(message);
            }
        });
    }

    public byte[] propose(byte[] value) throws InterruptedException {
        return proposer.propose(value);
    }

    public byte[] getLearnedValue() {
        return learner.getChosenValue();
    }
}
