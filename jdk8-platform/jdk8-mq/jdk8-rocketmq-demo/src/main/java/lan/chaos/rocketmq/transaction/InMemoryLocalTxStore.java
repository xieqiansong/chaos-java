package lan.chaos.rocketmq.transaction;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示用内存本地事务存储。
 * <p>
 * 仅用于演示"回查时查库"的调用结构；生产环境应查询真实订单表（见 {@link LocalTxStore}）。
 */
@Component
public class InMemoryLocalTxStore implements LocalTxStore {

    private final ConcurrentHashMap<String, Boolean> store = new ConcurrentHashMap<>();

    @Override
    public void mark(String bizKey, boolean committed) {
        store.put(bizKey, committed);
    }

    @Override
    public Boolean get(String bizKey) {
        return store.get(bizKey);
    }
}
