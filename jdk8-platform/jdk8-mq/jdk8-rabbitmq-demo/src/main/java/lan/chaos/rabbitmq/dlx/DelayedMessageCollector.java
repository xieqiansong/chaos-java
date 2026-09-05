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
 * 延迟目标队列监听器：接收缓冲队列 TTL 到期后被死信过来的消息（@Profile("!mock")）。
 *
 * <p>只负责接收并记录，供测试断言「消息确实延迟约 TTL 后才到达」。</p>
 */
@Slf4j
@Component
@org.springframework.context.annotation.Profile("!mock")
public class DelayedMessageCollector {

    @Getter
    private final ConcurrentLinkedQueue<OrderEvent> received = new ConcurrentLinkedQueue<>();

    @RabbitListener(queues = MqConstants.DELAY_TARGET_QUEUE)
    public void onDelayed(@Payload OrderEvent event,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        received.offer(event);
        channel.basicAck(deliveryTag, false);
        log.info("[delay] 延迟消息到达目标队列 orderId={}", event.getOrderId());
    }

    public void reset() {
        received.clear();
    }
}
