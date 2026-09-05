package lan.chaos.kafka.transaction;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 事务消费者：只读已被提交的事务消息（{@code isolation.level=read_committed}）。
 *
 * <p><b>read_committed vs read_uncommitted：</b><ul>
 *   <li>{@code read_committed}——只读已提交事务的消息，事务回滚期间写入的消息不可见；</li>
 *   <li>{@code read_uncommitted}（默认）——所有消息立即可见，包括未提交的事务消息。</li>
 * </ul></p>
 *
 * <p>设置方式：在 consumer 配置中 {@code spring.kafka.consumer.properties.isolation.level=read_committed}，
 * 或在测试 profile 中覆盖。</p>
 */
@Slf4j
@Component
public class TransactionConsumer {

    private final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

    @KafkaListener(
            topics = KafkaConstants.TOPIC_TRANSACTION,
            groupId = KafkaConstants.GROUP_TRANSACTION)
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("[tx] 消费已提交消息 | key={}, partition={}, offset={}, value={}",
                record.key(), record.partition(), record.offset(), record.value());
        received.offer(record.value());
    }

    public ConcurrentLinkedQueue<String> getReceived() {
        return received;
    }

    public void clear() {
        received.clear();
    }
}
