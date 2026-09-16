package lan.chaos.distributed.system.paxos.transport;
// ==================== 本地消息总线（模拟网络） ====================

import lan.chaos.distributed.system.paxos.message.PaxosMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalMessenger implements Messenger {
    private final Map<Integer, MessageHandler> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void send(PaxosMessage message) {
        executor.submit(() -> {
            MessageHandler handler = handlers.get(message.getTo());
            if (handler != null) {
                handler.onMessage(message);
            }
            // 广播给 -1 表示所有节点
            if (message.getTo() == -1) {
                for (Map.Entry<Integer, MessageHandler> entry : handlers.entrySet()) {
                    if (entry.getKey() != message.getFrom()) {
                        entry.getValue().onMessage(message);
                    }
                }
            }
        });
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        // 不直接绑定节点，由 PaxosNode 注册
    }

    public void registerNode(int nodeId, MessageHandler handler) {
        handlers.put(nodeId, handler);
    }
}
