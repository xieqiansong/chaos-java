package lan.chaos.rocketmq.batch;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 批量消息消费者：批量发送只是发送侧优化，这里仍逐条收到，正常解析并打印耗时。
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = MqConstant.TOPIC_BATCH, consumerGroup = MqConstant.GROUP_BATCH)
public class BatchConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("【批量消费】body={} | 耗时={}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
    }
}
