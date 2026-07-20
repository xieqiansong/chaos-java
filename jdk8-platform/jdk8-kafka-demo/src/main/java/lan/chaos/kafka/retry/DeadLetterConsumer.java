package lan.chaos.kafka.retry;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 死信消费者：收容重试耗尽后被投递到 {@code demo-retry-dlt} 的消息。
 *
 * <p><b>DLT 收容是强制要求：</b>不设 DLT = offset 永远卡住 = consumer 停止消费。
 * <p>生产环境 DLT 消息应写入监控/告警队列，由人工或自动化补偿。</p>
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    private final ConcurrentLinkedQueue<String> deadLetters = new ConcurrentLinkedQueue<>();

    @KafkaListener(
            topics = KafkaConstants.TOPIC_RETRY_DLT,
            groupId = KafkaConstants.GROUP_RETRY)
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        log.warn("[retry-DLT] 死信消息 | key={}, partition={}, offset={}, value={}",
                record.key(), record.partition(), record.offset(), record.value());
        deadLetters.offer(record.value());
    }

    public ConcurrentLinkedQueue<String> getDeadLetters() {
        return deadLetters;
    }

    public void clear() {
        deadLetters.clear();
    }
}
