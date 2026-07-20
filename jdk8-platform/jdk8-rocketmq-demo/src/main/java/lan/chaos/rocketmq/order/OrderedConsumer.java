package lan.chaos.rocketmq.order;

import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 顺序消息消费者：必须以 ORDERLY 模式消费，单队列串行，
 * 同一 orderId 的 CREATED / PAID / SHIPPED 才会严格按照发送顺序进入。
 * <p>
 * 注意：顺序消费下若某条消息处理失败抛异常，会阻塞该队列后续消息（直到成功），
 * 因此业务逻辑应尽量轻量、避免长耗时。消费时解析消息信封并打印耗时。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "demo-order-topic",
        consumerGroup = "demo-order-consumer-group",
        consumeMode = ConsumeMode.ORDERLY)
public class OrderedConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("【顺序消费】body={} | 耗时={}ms | thread={}",
                envelope.getBody(), MessageUtils.costMillis(envelope), Thread.currentThread().getName());
    }
}
