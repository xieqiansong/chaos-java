package lan.chaos.kafka.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import lan.chaos.kafka.common.constant.KafkaConstants;
import lan.chaos.kafka.common.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 有序消费者：按 orderId 分 partition 消费，同一订单内保证 CREATE → PAY → SHIP → DONE。
 *
 * <p><b>验证有序：</b>将每条消息按 orderId 归类，同一 orderId 下消息顺序必须按
 * CREATE→PAY→SHIP→DONE（或业务期望顺序）。测试断言的依据即此。</p>
 *
 * <p><b>并发消费：</b>默认 {@code concurrency = 1} 单线程消费保证有序；
 * 若调大并发度，同一 partition 仍只有一条线程消费（Kafka 内部保证），
 * 但不同 partition 并发处理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final ObjectMapper objectMapper;

    /** orderId → 事件列表（按接收顺序） */
    private final Map<String, List<String>> orderEvents = new ConcurrentHashMap<>();

    @KafkaListener(
            topics = KafkaConstants.TOPIC_ORDER,
            groupId = KafkaConstants.GROUP_ORDER)
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            OrderEvent event = objectMapper.readValue(record.value(), OrderEvent.class);
            log.info("[order] 消费 | orderId={}, action={}, partition={}, offset={}",
                    event.getOrderId(), event.getAction(), record.partition(), record.offset());

            orderEvents.computeIfAbsent(event.getOrderId(), k -> new ArrayList<>())
                    .add(event.getAction());
        } catch (Exception e) {
            log.error("[order] 反序列化失败: {}", record.value(), e);
        }
    }

    /** 获取某订单的事件顺序列表（测试断言用） */
    public List<String> getEventsFor(String orderId) {
        return orderEvents.getOrDefault(orderId, new ArrayList<>());
    }

    public void clear() {
        orderEvents.clear();
    }
}
