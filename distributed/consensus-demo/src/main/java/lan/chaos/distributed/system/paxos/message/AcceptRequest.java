package lan.chaos.distributed.system.paxos.message;

public class AcceptRequest extends PaxosMessage {
    private final ProposalId proposalId;
    private final byte[] value;

    public AcceptRequest(int from, int to, ProposalId proposalId, byte[] value) {
        super(from, to);
        this.proposalId = proposalId;
        this.value = value;
    }

    public ProposalId getProposalId() { return proposalId; }
    public byte[] getValue() { return value; }
}
