package lan.chaos.rocketmq.reliability;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.idempotent.MessageIdStore;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可靠性消费者：消息<b>不丢 + 不重</b>。
 *
 * <p><b>不丢（消费侧）：</b>RocketMQ 是 at-least-once，消费成功必须正常返回
 * （框架据此提交消费进度）；若处理抛异常，框架自动返回 {@code RECONSUME_LATER}，
 * Broker 会<b>重试投递</b>，直到达到最大重试次数进入死信——只要业务最终成功，消息就不会丢。</p>
 *
 * <p><b>不重：</b>重试是「同一条消息重投」（msgId 不变），用 {@link MessageIdStore}
 * 以 msgId 去重，避免重复消费造成重复副作用（如重复扣款）。</p>
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_RELIABILITY,
        consumerGroup = MqConstant.GROUP_RELIABILITY)
public class ReliabilityConsumer implements RocketMQListener<MessageExt> {

    @Resource
    private MessageIdStore messageIdStore;

    private final AtomicInteger processedCount = new AtomicInteger(0);

    @Override
    public void onMessage(MessageExt message) {
        String msgId = message.getMsgId();
        // 幂等：重试会重投同一 msgId，已在前面成功处理过的直接跳过
        if (messageIdStore.isProcessed(msgId)) {
            log.warn("⚠️ 重复消息，已跳过 | msgId={}", msgId);
            return;
        }
        Message envelope = MessageUtils.unpack(new String(message.getBody(), StandardCharsets.UTF_8));
        // 真实业务处理 ……
        log.info("✅ 可靠消费成功 | msgId={}, body={}, 耗时={}ms",
                msgId, envelope.getBody(), MessageUtils.costMillis(envelope));
        messageIdStore.markProcessed(msgId);
        processedCount.incrementAndGet();
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}
