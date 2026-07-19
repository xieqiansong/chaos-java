package lan.chaos.rocketmq.order;

import lan.chaos.rocketmq.message.Message;
import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 全局顺序消费者：consumeMode = ORDERLY，单线程按 Queue 顺序消费。
 * 配合生产者固定 hashKey 把消息都打到同一 Queue，即得到 topic 级严格全局有序。
 * 与 {@code OrderedConsumer}（分区有序）对照。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "demo-global-order-topic",
        consumerGroup = "demo-global-order-group",
        consumeMode = ConsumeMode.ORDERLY)
public class GlobalOrderConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("【全局顺序】收到 body={} | 耗时={}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
    }
}
