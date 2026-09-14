package lan.chaos.kafka.batch;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量生产者：一次性发送多条消息，减少网络往返。
 *
 * <p><b>Kafka 批量发送 vs RocketMQ：</b><ul>
 *   <li>Kafka 自带 {@code linger.ms} + {@code batch.size} 自动攒批——即使调用
 *       {@code send()} 一条条发，只要在 linger.ms 时间内有更多消息到达就会并批；</li>
 *   <li>此处演示「显式批量」：攒够 N 条后一次性 {@code send} 掉，
 *       适合定时/定量攒批再发的场景（如定时同步离线数据）。</li>
 * </ul></p>
 *
 * <p><b>大小限制：</b>单批总大小不超过 {@code max.request.size}（默认 1MB），
 * 超限会触发 {@code RecordTooLargeException}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 批量发送消息列表。
     *
     * @param messages 待发送的消息值列表
     * @return 成功发送条数
     */
    public int sendBatch(List<String> messages) {
        List<ProducerRecord<String, String>> records = new ArrayList<>();
        for (String msg : messages) {
            records.add(new ProducerRecord<>(KafkaConstants.TOPIC_BATCH, msg));
        }

        int success = 0;
        for (ProducerRecord<String, String> record : records) {
            try {
                kafkaTemplate.send(record).get();
                success++;
            } catch (Exception e) {
                log.error("[batch] 发送失败: {}", record.value(), e);
            }
        }
        log.info("[batch] 批量发送完成 | 总数={}, 成功={}", messages.size(), success);
        return success;
    }
}
