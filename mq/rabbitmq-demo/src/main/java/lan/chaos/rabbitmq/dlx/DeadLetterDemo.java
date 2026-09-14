package lan.chaos.rabbitmq.dlx;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * TTL + DLX 死信：消息在「工作队列」存活超时后，由 Broker 自动投递到 <b>死信交换机（DLX）</b>，
 * 再路由到死信队列（DLT），实现「重试 / 隔离异常消息」。
 *
 * <p><b>机制：</b>工作队列声明 {@code x-message-ttl}（存活时间）+ {@code x-dead-letter-exchange}
 * （死信去向）+ {@code x-dead-letter-routing-key}。消息到期无人消费即被 Broker 自动「死信」，
 * <b>无需业务代码抛异常</b>——这是与 Kafka 重试(代码层)的本质区别：RabbitMQ 死信是 Broker 原生能力。</p>
 *
 * <p><b>典型用途：</b>消费失败重试（配合重试队列）、延迟队列的退化实现、异常消息隔离告警。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterDemo {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OrderEvent event) {
        rabbitTemplate.convertAndSend(MqConstants.WORK_EXCHANGE, MqConstants.WORK_ROUTING, event);
        log.info("[dlx] 发布 orderId={} -> 工作队列（TTL={}ms 后自动死信到 DLT）",
                event.getOrderId(), MqConstants.WORK_TTL_MS);
    }
}
