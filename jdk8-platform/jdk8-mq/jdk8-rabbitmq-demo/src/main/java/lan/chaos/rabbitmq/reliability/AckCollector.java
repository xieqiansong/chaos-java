package lan.chaos.rabbitmq.reliability;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消费者手动 Ack 演示监听器（@Profile("!mock")：只在真实 Broker / *IT 启动，避免内存 Broker 干扰 *Test）。
 *
 * <p><b>演示要点：</b></p>
 * <ul>
 *   <li>首次收到消息 → 故意 {@code basicNack(requeue=true)} 重入队，模拟「处理失败需重试」；</li>
 *   <li>重投后第二次收到 → 业务处理完成 → {@code basicAck} 确认，消息从队列移除。</li>
 * </ul>
 * <p>通过 {@link #isAcked()} / {@link #getRequeueCount()} 供测试断言「先重入队、后手动 Ack」。</p>
 */
@Slf4j
@Component
@org.springframework.context.annotation.Profile("!mock")
public class AckCollector {

    @Getter
    private final ConcurrentLinkedQueue<OrderEvent> received = new ConcurrentLinkedQueue<>();
    @Getter
    private final AtomicInteger requeueCount = new AtomicInteger(0);
    @Getter
    private final AtomicBoolean acked = new AtomicBoolean(false);

    @RabbitListener(queues = MqConstants.ACK_QUEUE)
    public void onMessage(@Payload OrderEvent event,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // 首次投递：模拟失败 → nack 重入队（requeue=true）
        if (requeueCount.get() == 0) {
            requeueCount.incrementAndGet();
            log.info("[ack] 第 1 次收到 orderId={}，处理失败 → basicNack(requeue=true) 重入队",
                    event.getOrderId());
            channel.basicNack(deliveryTag, false, true);
            return;
        }
        // 重投后第 2 次：业务处理完成 → 手动 basicAck
        received.offer(event);
        acked.set(true);
        channel.basicAck(deliveryTag, false);
        log.info("[ack] 第 2 次收到 orderId={}，业务完成 → basicAck 确认", event.getOrderId());
    }

    /** 测试前重置状态，避免 DemoRunner 的演示消息干扰断言 */
    public void reset() {
        received.clear();
        requeueCount.set(0);
        acked.set(false);
    }
}
