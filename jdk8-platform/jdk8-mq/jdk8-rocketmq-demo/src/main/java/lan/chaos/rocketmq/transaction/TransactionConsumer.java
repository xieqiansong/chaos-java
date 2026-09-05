package lan.chaos.rocketmq.transaction;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 事务消息消费者：只有本地事务 COMMIT 后，半消息才会被投递到这里。
 * 消费时打印耗时。
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = MqConstant.TOPIC_TX, consumerGroup = MqConstant.GROUP_TX)
public class TransactionConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("收到事务最终提交的消息 | body={}, 耗时={}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
    }
}
