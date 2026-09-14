package lan.chaos.distributed.system.paxos.message;

public class ChosenNotification extends PaxosMessage {
    private final byte[] value;

    public ChosenNotification(int from, int to, byte[] value) {
        super(from, to);
        this.value = value;
    }

    public byte[] getValue() { return value; }
}
