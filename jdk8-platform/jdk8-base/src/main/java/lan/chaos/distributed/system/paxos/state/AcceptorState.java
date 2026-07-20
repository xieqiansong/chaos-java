package lan.chaos.distributed.system.paxos.state;

import lan.chaos.distributed.system.paxos.message.ProposalId;

public class AcceptorState {
    private ProposalId promisedId;
    private ProposalId acceptedId;
    private byte[] acceptedValue;

    public AcceptorState() {
        this.promisedId = new ProposalId(-1, -1);
        this.acceptedId = new ProposalId(-1, -1);
        this.acceptedValue = null;
    }

    public AcceptorState(ProposalId promisedId, ProposalId acceptedId, byte[] acceptedValue) {
        this.promisedId = promisedId;
        this.acceptedId = acceptedId;
        this.acceptedValue = acceptedValue;
    }

    public ProposalId getPromisedId() { return promisedId; }
    public void setPromisedId(ProposalId promisedId) { this.promisedId = promisedId; }
    public ProposalId getAcceptedId() { return acceptedId; }
    public void setAcceptedId(ProposalId acceptedId) { this.acceptedId = acceptedId; }
    public byte[] getAcceptedValue() { return acceptedValue; }
    public void setAcceptedValue(byte[] acceptedValue) { this.acceptedValue = acceptedValue; }
}
