package lan.chaos.rocketmq.broadcast;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 广播模式消费者（实例2）。与实例1用不同组名、同为 BROADCASTING，二者都会收到全量消息。
 * 与 BroadcastConsumer1 一起演示"广播 = 每个订阅实例都收到"。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_BROADCAST,
        consumerGroup = MqConstant.GROUP_BROADCAST_2,
        messageModel = MessageModel.BROADCASTING)
public class BroadcastConsumer2 implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("【广播消费-实例2】body={} | 耗时={}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
    }
}
