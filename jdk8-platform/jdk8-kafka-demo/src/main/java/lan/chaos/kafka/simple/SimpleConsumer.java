package lan.chaos.kafka.simple;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 基础消费者：演示手动提交 + 消息收集（供测试断言）。
 *
 * <p><b>手动提交 vs 自动提交：</b><ul>
 *   <li>自动提交（{@code enable-auto-commit: true}）——offset 周期性自动提交，
 *       简单但可能在消息处理完成前提交，崩溃后丢消息；</li>
 *   <li>手动提交——业务处理完成后显式 {@code ack.acknowledge()}，
 *       保证 at-least-once，是生产环境的常见选择。</li>
 * </ul></p>
 *
 * <p><b>单线程消费：</b>默认一个 partition 由一个线程消费，
 * 保证单 partition 内有序；多 partition 并发由 {@code concurrency} 控制。</p>
 */
@Slf4j
@Component
public class SimpleConsumer {

    /** 最近收到的消息（供测试断言使用） */
    private final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

    @KafkaListener(
            topics = KafkaConstants.TOPIC_SIMPLE,
            groupId = KafkaConstants.GROUP_SIMPLE)
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String msg = record.value();
        log.info("[simple] 收到消息 | key={}, partition={}, offset={}, value={}",
                record.key(), record.partition(), record.offset(), msg);
        received.offer(msg);
        // 业务处理完成后显式提交 offset
        ack.acknowledge();
    }

    /** 获取并清空已收消息（测试用） */
    public ConcurrentLinkedQueue<String> getReceived() {
        return received;
    }

    public void clear() {
        received.clear();
    }
}
