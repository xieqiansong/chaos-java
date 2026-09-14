package lan.chaos.kafka.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lan.chaos.kafka.common.constant.KafkaConstants;
import lan.chaos.kafka.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 有序消息生产者：同一 orderId 的所有事件发送到同一 partition，保证消费有序。
 *
 * <p><b>Kafka 如何保证有序：</b><ul>
 *   <li>Kafka 只在 <b>单个 partition 内保证有序</b>；</li>
 *   <li>{@code producer.send(new ProducerRecord(topic, key, value))} 中 key 相同 →
 *       hash 到同一 partition → 消费有序；</li>
 *   <li>与 RocketMQ 的 MessageQueue 选择器同理，只是粒度在 partition 上。</li>
 * </ul></p>
 *
 * <p><b>注意：</b>若 partition 数变化，同 key 会重新 hash 到不同 partition，
 * 导致「有序性被破坏」。</p>
 *
 * <p><b>与 RocketMQ 差异：</b>Kafka 的消费有序依赖单 partition + 单线程消费
 * （consumer per partition 模式）；若 {@code concurrency > 1} 多条线程
 * 并发消费同一 partition，有序性也会被打破——但这不常见。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 模拟一个订单的生命周期事件序列：CREATE → PAY → SHIP → DONE。
     * <p>四条消息使用相同的 orderId 作为 key，确保被路由到同一 partition。</p>
     *
     * @param orderId 订单号（作为 Kafka key，保证有序路由）
     */
    public void sendOrderLifecycle(String orderId) {
        List<OrderEvent> events = Arrays.asList(
                new OrderEvent(orderId, "CREATE", "订单已创建"),
                new OrderEvent(orderId, "PAY",   "已支付"),
                new OrderEvent(orderId, "SHIP",  "已发货"),
                new OrderEvent(orderId, "DONE",  "已完成")
        );

        for (OrderEvent event : events) {
            try {
                String json = objectMapper.writeValueAsString(event);
                // 用 orderId 作为 key → hash 到同一 partition → 保证有序
                SendResult<String, String> result =
                        kafkaTemplate.send(KafkaConstants.TOPIC_ORDER, orderId, json).get();
                log.info("[order] 发送 | orderId={}, action={}, partition={}, offset={}",
                        orderId, event.getAction(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } catch (Exception e) {
                log.error("[order] 发送失败 | orderId={}, action={}", orderId, event.getAction(), e);
            }
        }
        log.info("[order] 订单 {} 生命周期事件已全部发送", orderId);
    }
}
