package lan.chaos.distributed.system.paxos.transport;
// ==================== 网络抽象 ====================

import lan.chaos.distributed.system.paxos.message.PaxosMessage;

public interface Messenger {
    void send(PaxosMessage message);

    /** 注册消息处理器，节点收到消息时回调 */
    void setMessageHandler(MessageHandler handler);

    interface MessageHandler {
        void onMessage(PaxosMessage message);
    }
}
