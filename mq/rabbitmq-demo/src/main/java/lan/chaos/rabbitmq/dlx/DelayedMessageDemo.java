package lan.chaos.rabbitmq.dlx;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 延迟消息：用 <b>TTL 队列 + DLX</b> 组合实现「延迟投递」（RabbitMQ 原生无延迟交换机，
 * 官方 {@code rabbitmq-delayed-message-exchange} 插件另论）。
 *
 * <p><b>机制：</b>消息发布到「延迟缓冲队列」（带 {@code x-message-ttl}=延迟时长、
 * {@code x-dead-letter-exchange}=目标交换机）；缓冲队列不挂消费者，
 * 消息到期后被 Broker 死信到目标交换机，再路由到目标队列——即「延迟 N 毫秒后送达」。</p>
 *
 * <p><b>局限：</b>队列级 TTL 对所有消息同延迟；若需单条不同延迟，应改用消息级 TTL（每条设
 * {@code expiration}）或延迟插件。本 demo 用队列级 TTL 演示核心思路。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayedMessageDemo {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OrderEvent event) {
        // 发布到默认交换机、路由键=缓冲队列名（RabbitMQ 默认交换机直投同名队列）
        rabbitTemplate.convertAndSend("", MqConstants.DELAY_QUEUE, event);
        log.info("[delay] 发布 orderId={} -> 缓冲队列（TTL={}ms 后投递到目标队列）",
                event.getOrderId(), MqConstants.DELAY_TTL_MS);
    }
}
