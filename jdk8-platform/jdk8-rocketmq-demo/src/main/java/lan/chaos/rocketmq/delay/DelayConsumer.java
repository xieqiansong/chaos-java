package lan.chaos.rocketmq.delay;

import lan.chaos.rocketmq.message.Message;
import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 延迟消息消费者：消息会在达到对应 delayLevel 时间后才被投递到这里。
 * 消费时打印的耗时 ≈ 实际延迟时长。
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "demo-delay-topic", consumerGroup = "demo-delay-consumer-group")
public class DelayConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("收到延迟消息 | body={}, 实际延迟耗时≈{}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
    }
}
