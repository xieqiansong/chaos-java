package lan.chaos.distributed.system.paxos.role;
// ==================== Acceptor ====================

import lan.chaos.distributed.system.paxos.message.AcceptRequest;
import lan.chaos.distributed.system.paxos.message.AcceptResponse;
import lan.chaos.distributed.system.paxos.message.PrepareRequest;
import lan.chaos.distributed.system.paxos.message.PrepareResponse;
import lan.chaos.distributed.system.paxos.message.ProposalId;
import lan.chaos.distributed.system.paxos.message.PaxosMessage;
import lan.chaos.distributed.system.paxos.state.AcceptorState;
import lan.chaos.distributed.system.paxos.state.StatePersistence;
import lan.chaos.distributed.system.paxos.transport.Messenger;

public class Acceptor implements Messenger.MessageHandler {
    private final int nodeId;
    private final StatePersistence persistence;
    private final Messenger messenger;
    private AcceptorState state;

    public Acceptor(int nodeId, StatePersistence persistence, Messenger messenger) {
        this.nodeId = nodeId;
        this.persistence = persistence;
        this.messenger = messenger;
        this.state = persistence.load();
    }

    @Override
    public void onMessage(PaxosMessage message) {
        if (message instanceof PrepareRequest && message.getTo() == nodeId) {
            handlePrepare((PrepareRequest) message);
        } else if (message instanceof AcceptRequest && message.getTo() == nodeId) {
            handleAccept((AcceptRequest) message);
        }
    }

    private void handlePrepare(PrepareRequest req) {
        ProposalId pid = req.getProposalId();
        if (pid.compareTo(state.getPromisedId()) > 0) {
            state.setPromisedId(pid);
            persistence.save(state);  // 同步刷盘
            messenger.send(new PrepareResponse(nodeId, req.getFrom(), pid, true,
                    state.getAcceptedId(), state.getAcceptedValue()));
        } else {
            messenger.send(new PrepareResponse(nodeId, req.getFrom(), pid, false,
                    null, null));
        }
    }

    private void handleAccept(AcceptRequest req) {
        ProposalId pid = req.getProposalId();
        if (pid.compareTo(state.getPromisedId()) >= 0) {
            state.setPromisedId(pid);
            state.setAcceptedId(pid);
            state.setAcceptedValue(req.getValue());
            persistence.save(state);  // 同步刷盘
            messenger.send(new AcceptResponse(nodeId, req.getFrom(), pid, true));
        } else {
            messenger.send(new AcceptResponse(nodeId, req.getFrom(), pid, false));
        }
    }
}
