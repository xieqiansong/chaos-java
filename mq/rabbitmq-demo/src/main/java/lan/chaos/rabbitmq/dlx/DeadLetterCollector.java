package lan.chaos.rabbitmq.dlx;

import com.rabbitmq.client.Channel;
import lan.chaos.rabbitmq.common.constant.MqConstants;
import lan.chaos.rabbitmq.common.model.OrderEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 死信队列（DLT）监听器：收容工作队列 TTL 到期后被 Broker 自动死信的消息（@Profile("!mock")）。
 *
 * <p>只负责接收并记录，供测试断言「消息确实经 TTL 死信到达 DLT」。生产环境应配合告警 + 人工/自动补偿。</p>
 */
@Slf4j
@Component
@org.springframework.context.annotation.Profile("!mock")
public class DeadLetterCollector {

    @Getter
    private final ConcurrentLinkedQueue<OrderEvent> received = new ConcurrentLinkedQueue<>();

    @RabbitListener(queues = MqConstants.DLQ_QUEUE)
    public void onDeadLetter(@Payload OrderEvent event,
                             Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        received.offer(event);
        channel.basicAck(deliveryTag, false);
        log.info("[dlx] 死信到达 DLT orderId={}", event.getOrderId());
    }

    public void reset() {
        received.clear();
    }
}
