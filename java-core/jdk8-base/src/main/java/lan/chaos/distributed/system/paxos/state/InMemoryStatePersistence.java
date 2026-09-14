package lan.chaos.distributed.system.paxos.state;

/** 内存实现，生产环境可替换为 RocksDB 或文件日志 */
public class InMemoryStatePersistence implements StatePersistence {
    private AcceptorState acceptorState = new AcceptorState();
    private long currentRound = 0;

    @Override
    public synchronized AcceptorState load() {
        return new AcceptorState(acceptorState.getPromisedId(),
                acceptorState.getAcceptedId(), acceptorState.getAcceptedValue());
    }

    @Override
    public synchronized void save(AcceptorState state) {
        this.acceptorState = new AcceptorState(state.getPromisedId(),
                state.getAcceptedId(), state.getAcceptedValue());
    }

    @Override
    public synchronized long loadCurrentRound() {
        return currentRound;
    }

    @Override
    public synchronized void saveCurrentRound(long round) {
        this.currentRound = round;
    }
}
