package lan.chaos.rocketmq.requestreply;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQReplyListener;
import org.springframework.stereotype.Component;

/**
 * 请求-应答消费者：实现 RocketMQReplyListener，onMessage 的返回值即回传给生产者的结果。
 * 与 {@code RequestReplyProducer} 配套。
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "demo-rr-topic", consumerGroup = "demo-rr-group")
public class RequestReplyConsumer implements RocketMQReplyListener<String, String> {

    @Override
    public String onMessage(String message) {
        log.info("【请求应答-消费】收到请求={}", message);
        return "ack:" + message;
    }
}
