package lan.chaos.rocketmq.common;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示用内存去重存储。
 * <p>
 * 注意：进程重启后去重状态丢失，且多实例间不共享。
 * 生产环境请替换为 Redis / 数据库唯一键实现（见 {@link MessageIdStore} 说明）。
 */
@Component
public class InMemoryMessageIdStore implements MessageIdStore {

    private final ConcurrentHashMap<String, Boolean> processed = new ConcurrentHashMap<>();

    @Override
    public boolean isProcessed(String msgId) {
        return processed.containsKey(msgId);
    }

    @Override
    public void markProcessed(String msgId) {
        processed.put(msgId, Boolean.TRUE);
    }
}
