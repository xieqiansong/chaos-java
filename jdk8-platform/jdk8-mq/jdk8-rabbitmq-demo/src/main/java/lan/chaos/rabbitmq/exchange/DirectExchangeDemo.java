package lan.chaos.rabbitmq.exchange;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Direct 交换机：精确匹配路由键（1:1 或 1:N 但键完全一致）。
 *
 * <p><b>机制：</b>消息的 routingKey 必须 <b>完全等于</b> 队列绑定时的 binding key 才会投递。
 * 典型用途：点对点任务分发、按业务键精确路由。</p>
 *
 * <p><b>验证方式：</b>本 demo 用 {@code RabbitTemplate.receive(queue, timeout)} 同步拉取，
 * 无需监听器即可断言路由结果（自包含 *Test 也走此路径）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectExchangeDemo {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布一条消息到 direct 交换机并用 routing key 精确路由，同步拉回验证。
     *
     * @return 路由到的消息（未收到返回 null）
     */
    public OrderEvent route(OrderEvent event) {
        rabbitTemplate.convertAndSend(
                MqConstants.DIRECT_EXCHANGE, MqConstants.DIRECT_ROUTING, event);
        log.info("[direct] 发布 orderId={} -> exchange={}, routingKey={}",
                event.getOrderId(), MqConstants.DIRECT_EXCHANGE, MqConstants.DIRECT_ROUTING);

        OrderEvent received = (OrderEvent) rabbitTemplate.receiveAndConvert(
                MqConstants.DIRECT_QUEUE, 3000L);
        log.info("[direct] 从队列 {} 拉回: {}", MqConstants.DIRECT_QUEUE, received);
        return received;
    }
}
