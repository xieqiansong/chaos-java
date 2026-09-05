package lan.chaos.rocketmq.retry;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者。
 * <p>
 * 重试达到上限后，消息进入死信 Topic：%DLQ% + 原消费组名。
 * 死信必须由"全新消费组"来消费，进行人工补偿（落补偿表 / 告警 / 人工审核）。
 * 消费时打印从发送到落入死信的耗时。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.DLQ_TOPIC_RETRY,
        consumerGroup = MqConstant.GROUP_DLQ_HANDLER)
public class DeadLetterConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.error("⚠️ 收到死信消息，进入人工补偿流程 | body={}, 从发送到死信耗时={}ms",
                envelope.getBody(), MessageUtils.costMillis(envelope));
        // 例如：insert into compensation_task(task_data) values (?)
    }
}
