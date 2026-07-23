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
 * 广播模式「消费失败不重试」演示（概念场景）。
 * <p>
 * 关键差异：
 * <ul>
 *     <li>集群模式（CLUSTERING）：消费抛异常会触发【重试】，走 %RETRY% 主题，直到达到最大重试次数进入死信；</li>
 *     <li>广播模式（BROADCASTING）：消费抛异常【不会重试】，消息直接丢弃（每个实例都只投一次）。</li>
 * </ul>
 * 因此广播场景下必须自己做好兜底（本地落盘 / 告警 / 业务幂等补偿），不能依赖 RocketMQ 的重投机制。
 * <p>
 * 本消费者演示：收到正文为 "fail" 的消息时故意抛异常，运行日志里只会看到【一次】处理记录，
 * 不会出现重复消费 / 重试日志，从而印证"广播不重试"。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_BROADCAST_NO_RETRY,
        consumerGroup = MqConstant.GROUP_BROADCAST_NO_RETRY,
        messageModel = MessageModel.BROADCASTING)
public class BroadcastNoRetryConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String payload) {
        Message envelope = MessageUtils.unpack(payload);
        log.info("【广播-不重试】收到 body={}", envelope.getBody());
        if ("fail".equals(envelope.getBody())) {
            // 广播模式下抛异常不会触发重投，只会被丢弃；用于印证「广播不重试」
            throw new RuntimeException("模拟广播消费失败（不会重试）");
        }
    }
}
