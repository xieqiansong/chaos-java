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
 * 广播模式消费者（实例1）。messageModel=BROADCASTING，每条消息都会投递给所有订阅该 topic 的实例，
 * 因此实例1、实例2都会各收到一份（全量投递）。
 * <p>
 * 两个实例必须用【不同的 consumerGroup】：同一个 Spring 上下文里两个 @RocketMQMessageListener 不能共用同一组名，
 * 否则会报 "consumer group has been created before"。即便组名不同，广播模式下二者仍各自收到全量消息。
 * <p>
 * 对照：若改为默认的 MessageModel.CLUSTERING（集群/负载均衡），同一个组内的多个实例会分摊消息（各自只收到一部分）。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_BROADCAST,
        consumerGroup = MqConstant.GROUP_BROADCAST_1,
        messageModel = MessageModel.BROADCASTING)
public class BroadcastConsumer1 implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("【广播消费-实例1】body={} | 耗时={}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
    }
}
