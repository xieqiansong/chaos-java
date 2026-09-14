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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 幂等消费（不重）演示监听器（@Profile("!mock")：仅真实 Broker 下运行）。
 *
 * <p><b>为什么需要：</b>RabbitMQ 手动 Ack 下是 at-least-once，网络抖动 / 消费者重启 /
 * 生产者重发都会造成同一条业务消息被<b>重复投递</b>。业务层必须用<b>业务键去重</b>避免重复副作用。</p>
 *
 * <p><b>演示要点：</b>以 {@code orderId} 作为业务幂等键，用内存 {@code Set} 记录已处理键；
 * 重复投递时命中已处理集合 → 直接 {@code basicAck}（必须 ack，否则会反复重投）并跳过业务。
 * 通过 {@link #getReceivedCount()} / {@link #getProcessedCount()} 供测试断言「收到 2 次、仅处理 1 次」。</p>
 */
@Slf4j
@Component
@org.springframework.context.annotation.Profile("!mock")
public class IdempotentCollector {

    @Getter
    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();
    @Getter
    private final AtomicInteger receivedCount = new AtomicInteger(0);
    @Getter
    private final AtomicInteger processedCount = new AtomicInteger(0);

    @RabbitListener(queues = MqConstants.IDEMPOTENT_QUEUE)
    public void onMessage(@Payload OrderEvent event,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        receivedCount.incrementAndGet();
        String bizKey = event.getOrderId();   // 业务幂等键
        if (processedKeys.contains(bizKey)) {
            log.warn("[idempotent] 重复投递 orderId={}，已处理过 → 直接 ack 跳过", bizKey);
            channel.basicAck(deliveryTag, false);
            return;
        }
        // 首次：执行业务（如写库 / 扣减），成功后记录幂等键再 ack
        log.info("[idempotent] 首次处理 orderId={}，业务执行中…", bizKey);
        processedKeys.add(bizKey);
        processedCount.incrementAndGet();
        channel.basicAck(deliveryTag, false);
        log.info("[idempotent] 业务完成并 basicAck | orderId={}", bizKey);
    }

    public void reset() {
        processedKeys.clear();
        receivedCount.set(0);
        processedCount.set(0);
    }
}
