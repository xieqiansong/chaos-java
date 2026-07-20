package lan.chaos.distributed.system.paxos.message;

public class PrepareResponse extends PaxosMessage {
    private final ProposalId proposalId;
    private final ProposalId acceptedId;
    private final byte[] acceptedValue;
    private final boolean ok;

    public PrepareResponse(int from, int to, ProposalId proposalId, boolean ok,
                           ProposalId acceptedId, byte[] acceptedValue) {
        super(from, to);
        this.proposalId = proposalId;
        this.ok = ok;
        this.acceptedId = acceptedId;
        this.acceptedValue = acceptedValue;
    }

    public ProposalId getProposalId() { return proposalId; }
    public boolean isOk() { return ok; }
    public ProposalId getAcceptedId() { return acceptedId; }
    public byte[] getAcceptedValue() { return acceptedValue; }
}
