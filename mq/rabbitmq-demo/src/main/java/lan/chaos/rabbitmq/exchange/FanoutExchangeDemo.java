package lan.chaos.rabbitmq.exchange;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Fanout 交换机：<b>广播</b>——忽略 routing key，把消息投递给所有绑定的队列（每队列一份拷贝）。
 *
 * <p><b>机制：</b>绑定即订阅，发布到 fanout 的消息会复制到每个绑定队列。
 * 典型用途：修缓存、事件通知、发布/订阅。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FanoutExchangeDemo {

    private final RabbitTemplate rabbitTemplate;

    public void broadcast(OrderEvent event) {
        // fanout 忽略 routing key，传 "" 即可
        rabbitTemplate.convertAndSend(MqConstants.FANOUT_EXCHANGE, "", event);
        log.info("[fanout] 广播 orderId={} -> 所有绑定队列", event.getOrderId());
    }

    public OrderEvent receiveFromA() {
        return (OrderEvent) rabbitTemplate.receiveAndConvert(MqConstants.FANOUT_QUEUE_A, 3000L);
    }

    public OrderEvent receiveFromB() {
        return (OrderEvent) rabbitTemplate.receiveAndConvert(MqConstants.FANOUT_QUEUE_B, 3000L);
    }
}
