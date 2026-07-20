package lan.chaos.kafka.batch;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 批量消费者：一次拉取一批消息（{@code List<ConsumerRecord>}），
 * 适合需要批量处理下游（如批量写库、批量聚合）的场景。
 *
 * <p><b>启用条件：</b>需在 {@code application.yml} 的 consumer 配置中设置
 * {@code type: batch}（Spring Boot 自动配置），或通过
 * {@code @Bean factory.setBatchListener(true)} 手动开启。
 * 此处用 {@code spring.kafka.listener.type=batch} 全局批量消费模式，
 * 由 {@code application.yml} 统一控制。</p>
 *
 * <p><b>注意：</b>批量模式下 offset 在批次处理完一次性提交，
 * 若批次处理中部分失败则整批 retry。</p>
 */
@Slf4j
@Component
public class BatchConsumer {

    private final ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

    /**
     * 批量消费——一次性接收一批 ConsumerRecord。
     * <p>测试时由 @EmbeddedKafka 的 application.yml profile 覆盖开启 batch listener。
     * 生产代码里此 listener 在非 batch 模式下不会触发。</p>
     */
    @KafkaListener(
            topics = KafkaConstants.TOPIC_BATCH,
            groupId = KafkaConstants.GROUP_BATCH,
            containerFactory = "batchContainerFactory")
    public void onBatch(List<ConsumerRecord<String, String>> records) {
        log.info("[batch] 收到一批 {} 条消息", records.size());
        for (ConsumerRecord<String, String> r : records) {
            received.offer(r.value());
            log.info("[batch]   -> partition={}, offset={}, value={}",
                    r.partition(), r.offset(), r.value());
        }
    }

    public ConcurrentLinkedQueue<String> getReceived() {
        return received;
    }

    public void clear() {
        received.clear();
    }
}
