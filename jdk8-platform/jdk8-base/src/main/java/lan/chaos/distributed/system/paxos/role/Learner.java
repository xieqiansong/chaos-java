package lan.chaos.distributed.system.paxos.role;
// ==================== Learner ====================

import lan.chaos.distributed.system.paxos.message.ChosenNotification;
import lan.chaos.distributed.system.paxos.message.PaxosMessage;
import lan.chaos.distributed.system.paxos.transport.Messenger;

public class Learner implements Messenger.MessageHandler {
    private final int nodeId;
    private byte[] chosenValue;

    public Learner(int nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void onMessage(PaxosMessage message) {
        if (message instanceof ChosenNotification && (message.getTo() == nodeId || message.getTo() == -1)) {
            this.chosenValue = ((ChosenNotification) message).getValue();
            System.out.println("Learner " + nodeId + " learned value: " + new String(chosenValue));
        }
    }

    public byte[] getChosenValue() {
        return chosenValue;
    }
}
