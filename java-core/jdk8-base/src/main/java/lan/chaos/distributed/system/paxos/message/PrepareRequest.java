package lan.chaos.distributed.system.paxos.message;

public class PrepareRequest extends PaxosMessage {
    private final ProposalId proposalId;

    public PrepareRequest(int from, int to, ProposalId proposalId) {
        super(from, to);
        this.proposalId = proposalId;
    }

    public ProposalId getProposalId() { return proposalId; }
}
