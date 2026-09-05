package lan.chaos.rabbitmq.reliability;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 幂等消费演示发布者：<b>故意用同一 orderId 发送两次</b>，模拟「生产者重发 / Broker 重投」，
 * 验证消费端只真正处理一次（不重）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentDemo {

    private final RabbitTemplate rabbitTemplate;

    public void publishDuplicate(String orderId) {
        rabbitTemplate.convertAndSend(MqConstants.IDEMPOTENT_EXCHANGE, MqConstants.IDEMPOTENT_ROUTING, OrderEvent.sample(orderId));
        rabbitTemplate.convertAndSend(MqConstants.IDEMPOTENT_EXCHANGE, MqConstants.IDEMPOTENT_ROUTING, OrderEvent.sample(orderId));
        log.info("[idempotent] 已用同一 orderId={} 发送 2 次，验证消费端去重", orderId);
    }
}
