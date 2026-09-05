package lan.chaos.rabbitmq.reliability;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * 消费者手动 Ack：业务处理完成后再显式确认，保证 at-least-once。
 *
 * <p><b>机制：</b>监听器容器设为 {@code AcknowledgeMode.MANUAL} 后，消息不会自动 ack，
 * 需业务处理后调用 {@code Channel.basicAck}；处理失败可 {@code basicNack(requeue=true)}
 * 让消息重入队重试，或 {@code requeue=false} 进死信。</p>
 *
 * <p>本场景的「收到 + 手动 ack + 首次 nack 重入队」由 {@link AckCollector} 演示，
 * 此类仅负责发布，便于测试断言。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerAckDemo {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OrderEvent event) {
        rabbitTemplate.convertAndSend(MqConstants.ACK_EXCHANGE, MqConstants.ACK_ROUTING, event);
        log.info("[ack] 发布 orderId={} -> 等待消费者手动 Ack", event.getOrderId());
    }
}
