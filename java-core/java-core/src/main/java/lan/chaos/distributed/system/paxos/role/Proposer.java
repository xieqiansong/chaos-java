package lan.chaos.distributed.system.paxos.role;
// ==================== Proposer ====================

import lan.chaos.distributed.system.paxos.message.AcceptRequest;
import lan.chaos.distributed.system.paxos.message.AcceptResponse;
import lan.chaos.distributed.system.paxos.message.ChosenNotification;
import lan.chaos.distributed.system.paxos.message.PrepareRequest;
import lan.chaos.distributed.system.paxos.message.PrepareResponse;
import lan.chaos.distributed.system.paxos.message.ProposalId;
import lan.chaos.distributed.system.paxos.message.PaxosMessage;
import lan.chaos.distributed.system.paxos.state.StatePersistence;
import lan.chaos.distributed.system.paxos.transport.Messenger;

public class Proposer implements Messenger.MessageHandler {
    private final int nodeId;
    private final StatePersistence persistence;
    private final Messenger messenger;
    private final int quorum;
    private final long timeoutMs;
    private final int clusterSize;

    // 当前提案上下文
    private byte[] proposedValue;
    private ProposalId currentProposalId;
    private int prepareOkCount;
    private ProposalId highestAcceptedId;
    private byte[] highestAcceptedValue;
    private int acceptOkCount;

    public Proposer(int nodeId, StatePersistence persistence, Messenger messenger,
                    int clusterSize, long timeoutMs) {
        this.nodeId = nodeId;
        this.persistence = persistence;
        this.messenger = messenger;
        this.clusterSize = clusterSize;
        this.quorum = clusterSize / 2 + 1;
        this.timeoutMs = timeoutMs;
    }

    /** 发起一次新的共识提议，会阻塞直到决议被选定或最终失败 */
    public byte[] propose(byte[] value) throws InterruptedException {
        proposedValue = value;
        while (true) {
            currentProposalId = generateProposalId();
            prepareOkCount = 0;
            highestAcceptedId = new ProposalId(-1, -1);
            highestAcceptedValue = null;
            acceptOkCount = 0;

            // 阶段 1: Prepare
            broadcastPrepare();
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline && prepareOkCount < quorum) {
                Thread.sleep(10); // 简化的等待，实际应使用 CountDownLatch
            }
            if (prepareOkCount < quorum) {
                continue; // 超时重试，下一轮编号更大
            }

            // 选择值：若存在已接受值，使用编号最大的那个
            byte[] valueToPropose = highestAcceptedValue != null ? highestAcceptedValue : proposedValue;

            // 阶段 2: Accept
            broadcastAccept(valueToPropose);
            deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline && acceptOkCount < quorum) {
                Thread.sleep(10);
            }
            if (acceptOkCount >= quorum) {
                // 值已选定，通知所有 Learner（通过广播）
                messenger.send(new ChosenNotification(nodeId, -1, valueToPropose));
                return valueToPropose;
            }
            // 否则重试
        }
    }

    private ProposalId generateProposalId() {
        long round = persistence.loadCurrentRound() + 1;
        persistence.saveCurrentRound(round);
        return new ProposalId(round, nodeId);
    }

    private void broadcastPrepare() {
        for (int i = 0; i < clusterSize; i++) {
            messenger.send(new PrepareRequest(nodeId, i, currentProposalId));
        }
    }

    private void broadcastAccept(byte[] value) {
        for (int i = 0; i < clusterSize; i++) {
            messenger.send(new AcceptRequest(nodeId, i, currentProposalId, value));
        }
    }

    @Override
    public void onMessage(PaxosMessage message) {
        if (message instanceof PrepareResponse && message.getTo() == nodeId) {
            PrepareResponse resp = (PrepareResponse) message;
            if (resp.getProposalId().equals(currentProposalId) && resp.isOk()) {
                prepareOkCount++;
                if (resp.getAcceptedId() != null &&
                        resp.getAcceptedId().compareTo(highestAcceptedId) > 0) {
                    highestAcceptedId = resp.getAcceptedId();
                    highestAcceptedValue = resp.getAcceptedValue();
                }
            }
        } else if (message instanceof AcceptResponse && message.getTo() == nodeId) {
            AcceptResponse resp = (AcceptResponse) message;
            if (resp.getProposalId().equals(currentProposalId) && resp.isOk()) {
                acceptOkCount++;
            }
        }
    }
}
