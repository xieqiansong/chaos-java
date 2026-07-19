package lan.chaos.rocketmq.retry;

import lan.chaos.rocketmq.common.MessageIdStore;
import lan.chaos.rocketmq.message.Message;
import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * 重试 + 幂等消费示例。
 * <p>
 * <ul>
 *     <li>抛异常即触发重试（RECONSUME_LATER），重试达到 maxReconsumeTimes 后进入死信队列；</li>
 *     <li>重试是"同一条消息重新投递"，msgId 不变，因此先用 MessageIdStore 去重，避免重复消费造成 side effect；</li>
 *     <li>负责计数的是 MessageIdStore，避免并发下失真。</li>
 * </ul>
 * 消费时解析消息信封并打印耗时。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "demo-retry-topic",
        consumerGroup = "demo-retry-consumer-group",
        maxReconsumeTimes = 3)
public class RetryConsumer implements RocketMQListener<MessageExt> {

    @Resource
    private MessageIdStore messageIdStore;

    @Override
    public void onMessage(MessageExt message) {
        String msgId = message.getMsgId();
        // 幂等：重试会重投同一 msgId，已在前面成功处理过的直接跳过
        if (messageIdStore.isProcessed(msgId)) {
            log.warn("⚠️ 重复消息，已跳过 | msgId={}", msgId);
            return;
        }
        Message envelope = MessageUtils.unpack(new String(message.getBody(), StandardCharsets.UTF_8));
        if (envelope.getBody().contains("error")) {
            log.error("消费失败，将触发重试 | msgId={}, 耗时={}ms", msgId, MessageUtils.costMillis(envelope));
            throw new RuntimeException("模拟消费失败，需重试");
        }
        log.info("✅ 消费成功 | msgId={}, body={}, 耗时={}ms", msgId, envelope.getBody(), MessageUtils.costMillis(envelope));
        messageIdStore.markProcessed(msgId);
    }
}
