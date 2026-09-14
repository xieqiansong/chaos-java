package lan.chaos.kafka.filter;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 过滤消费者：按消息 header 中的 "type" 标签做本地过滤，
 * 只处理 type=ORDER 的消息，其他类型跳过并记日志。
 *
 * <p><b>注意：</b>所有消息仍被消费（offset 前进），只是业务逻辑跳过不合规的。
 * 这与 RocketMQ Tag 过滤（服务端拦截，消费者收不到非匹配消息）不同。</p>
 */
@Slf4j
@Component
public class FilterConsumer {

    private final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

    @KafkaListener(
            topics = KafkaConstants.TOPIC_FILTER,
            groupId = KafkaConstants.GROUP_FILTER)
    public void onMessage(ConsumerRecord<String, String> record) {
        String type = getHeader(record, "type");

        if (!"ORDER".equals(type)) {
            log.info("[filter] 跳过非 ORDER 消息 | type={}, value={}", type, record.value());
            return;
        }

        log.info("[filter] 处理 ORDER 消息 | key={}, partition={}, value={}",
                record.key(), record.partition(), record.value());
        received.offer(record.value());
    }

    private String getHeader(ConsumerRecord<?, ?> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
    }

    public ConcurrentLinkedQueue<String> getReceived() {
        return received;
    }

    public void clear() {
        received.clear();
    }
}
