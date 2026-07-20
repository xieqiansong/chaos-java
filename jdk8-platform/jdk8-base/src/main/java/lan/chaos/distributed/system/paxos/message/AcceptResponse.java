package lan.chaos.distributed.system.paxos.message;

public class AcceptResponse extends PaxosMessage {
    private final ProposalId proposalId;
    private final boolean ok;

    public AcceptResponse(int from, int to, ProposalId proposalId, boolean ok) {
        super(from, to);
        this.proposalId = proposalId;
        this.ok = ok;
    }

    public ProposalId getProposalId() { return proposalId; }
    public boolean isOk() { return ok; }
}
