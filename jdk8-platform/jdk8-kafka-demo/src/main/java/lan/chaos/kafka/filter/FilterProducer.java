package lan.chaos.kafka.filter;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 过滤演示生产者：发送带 header 标签的消息，消费端按 header 做路由/过滤。
 *
 * <p><b>Kafka 消息过滤的几种方式：</b><ul>
 *   <li><b>Header 过滤（推荐）</b>——消息头存标签，消费端本地判断（零 Broker 开销）；</li>
 *   <li>RecordFilterStrategy——Spring Kafka 提供的消费者侧过滤策略；</li>
 *   <li>多 Topic 路由——不同业务类型发不同 Topic（最可靠但 Topic 膨胀）；</li>
 *   <li>原生无服务端过滤——不像 RocketMQ 有 Tag/SQL92 服务端过滤。</li>
 * </ul></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilterProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送带类型标签的消息。
     *
     * @param key  消息 key
     * @param body 消息体
     * @param type 消息类型（ORDER / LOG / ALERT），消费端按此过滤
     */
    public void sendWithHeader(String key, String body, String type) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaConstants.TOPIC_FILTER, null, key, body);
        record.headers().add(new RecordHeader("type", type.getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(record);
        log.info("[filter] 发送 | key={}, type={}, value={}", key, type, body);
    }
}
