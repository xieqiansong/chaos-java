package lan.chaos.rocketmq.order;

import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 顺序消息（分区顺序）生产者。
 * <p>
 * 原理：相同 hashKey（这里用 orderId）经哈希取模，永远路由到同一队列；
 * 配合消费端 ConsumeMode.ORDERLY 单队列串行消费，从而保证"同一订单内"消息有序。
 * <p>
 * 注意：这是"分区顺序"，不是"全局顺序"——不同 orderId 之间不保证顺序。
 * 若需要全局顺序，需将 Topic 队列数设为 1（代价是失去并行度）。
 */
@Slf4j
@Service
public class OrderedProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-order-topic";

    public void sendOrderLifecycle(String orderId) {
        for (String status : new String[]{"CREATED", "PAID", "SHIPPED"}) {
            String payload = MessageUtils.pack(String.format("订单:%s, 状态:%s", orderId, status));
            Message<String> msg = MessageBuilder
                    .withPayload(payload)
                    .setHeader("orderId", orderId)
                    .build();
            SendResult r = rocketMQTemplate.syncSendOrderly(TOPIC, msg, orderId);
            log.info("发送顺序消息 | orderId={}, status={}, queueId={}",
                    orderId, status, r.getMessageQueue().getQueueId());
        }
    }
}
