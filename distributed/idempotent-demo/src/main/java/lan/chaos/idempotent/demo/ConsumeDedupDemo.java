package lan.chaos.idempotent.demo;

import lan.chaos.idempotent.common.util.SampleFactory;
import lan.chaos.idempotent.core.ConsumeIdempotentGuard;

/**
 * 场景二：消费级重复投递。
 * 模拟 MQ 因未收到 ack 重发同一条消息（messageId 不变）。
 * 期望：同 messageId 只被消费一次。
 */
public class ConsumeDedupDemo {

    private final ConsumeIdempotentGuard guard;

    public ConsumeDedupDemo(ConsumeIdempotentGuard guard) {
        this.guard = guard;
    }

    public String run() {
        String messageId = SampleFactory.newMessageId();
        String bizNo = SampleFactory.newBizNo();
        // 第一次投递
        guard.consume(messageId, bizNo, mid -> System.out.println("  处理业务事件 " + bizNo));
        // 重试投递（同 messageId）
        guard.consume(messageId, bizNo, mid -> System.out.println("  处理业务事件 " + bizNo));
        return "messageId=" + messageId + " 实际消费次数=" + guard.consumedCount();
    }
}
