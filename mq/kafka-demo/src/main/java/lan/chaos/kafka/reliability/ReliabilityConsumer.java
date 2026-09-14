package lan.chaos.kafka.reliability;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可靠性消费者：消息<b>不丢（消费侧）</b>。
 *
 * <p><b>应用层不丢的关键：</b></p>
 * <ul>
 *   <li>关闭自动提交（application.yml 已 {@code enable-auto-commit: false}），
 *       改为<b>业务处理完成后再手动 {@code ack.acknowledge()}</b>；</li>
 *   <li>若处理中途抛异常、未调用 ack，offset 不会提交，<b>消费者重启/重平衡后会重新拉取该消息</b>，
 *       不会「offset 已提交却没处理完」导致丢消息（at-least-once）；</li>
 *   <li>代价是可能重复消费，需业务幂等（见 transaction 场景 / README 幂等说明）。</li>
 * </ul>
 */
@Slf4j
@Component
public class ReliabilityConsumer {

    private final ConcurrentLinkedQueue<String> processed = new ConcurrentLinkedQueue<>();
    private final AtomicInteger receivedCount = new AtomicInteger(0);
    private final AtomicInteger processedCount = new AtomicInteger(0);

    @KafkaListener(
            topics = KafkaConstants.TOPIC_RELIABILITY,
            groupId = KafkaConstants.GROUP_RELIABILITY)
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        receivedCount.incrementAndGet();
        String msg = record.value();
        try {
            // 模拟业务处理；若此处抛异常，下面 ack 不执行 → offset 不提交 → 重启后重投
            log.info("[reliability] 处理中 | key={}, offset={}, value={}",
                    record.key(), record.offset(), msg);
            processed.offer(msg);
            processedCount.incrementAndGet();
            // 业务成功后才提交 offset —— 这是「不丢」的保证
            ack.acknowledge();
            log.info("[reliability] 处理完成并提交 offset | key={}", record.key());
        } catch (Exception e) {
            // 注意：不调用 ack.acknowledge()
            log.error("[reliability] 处理失败，未提交 offset（重启后将重投）| key={}", record.key(), e);
        }
    }

    public ConcurrentLinkedQueue<String> getProcessed() {
        return processed;
    }

    public int getReceivedCount() {
        return receivedCount.get();
    }

    public int getProcessedCount() {
        return processedCount.get();
    }

    public void clear() {
        processed.clear();
        receivedCount.set(0);
        processedCount.set(0);
    }
}
