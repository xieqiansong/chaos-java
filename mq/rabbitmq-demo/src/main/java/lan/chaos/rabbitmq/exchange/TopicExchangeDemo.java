package lan.chaos.rabbitmq.exchange;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Topic 交换机：按 routing key 的 <b>点分通配符</b> 匹配（{@code *}=一个词，{@code #}=零或多词）。
 *
 * <p><b>机制：</b>绑定键用 {@code order.*} / {@code log.*} / {@code #} 等模式，
 * 发布键 {@code order.created} 同时命中 {@code order.*} 与 {@code #} 两个队列。
 * 典型用途：日志分级、事件分类订阅。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicExchangeDemo {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(OrderEvent event) {
        rabbitTemplate.convertAndSend(
                MqConstants.TOPIC_EXCHANGE, MqConstants.TOPIC_RK_ORDER_PREFIX + "created", event);
        log.info("[topic] 发布订单事件 orderId={} -> routingKey=order.created", event.getOrderId());
    }

    public void publishLogInfo(OrderEvent event) {
        rabbitTemplate.convertAndSend(
                MqConstants.TOPIC_EXCHANGE, MqConstants.TOPIC_RK_LOG_PREFIX + "info", event);
        log.info("[topic] 发布日志事件 orderId={} -> routingKey=log.info", event.getOrderId());
    }

    /** 从订单队列同步拉取一条（无则返回 null） */
    public OrderEvent receiveFromOrders() {
        return (OrderEvent) rabbitTemplate.receiveAndConvert(MqConstants.TOPIC_QUEUE_ORDERS, 2000L);
    }

    /** 从日志队列同步拉取一条 */
    public OrderEvent receiveFromLogs() {
        return (OrderEvent) rabbitTemplate.receiveAndConvert(MqConstants.TOPIC_QUEUE_LOGS, 2000L);
    }

    /** 从「全部」队列同步拉取一条 */
    public OrderEvent receiveFromAll() {
        return (OrderEvent) rabbitTemplate.receiveAndConvert(MqConstants.TOPIC_QUEUE_ALL, 2000L);
    }
}
