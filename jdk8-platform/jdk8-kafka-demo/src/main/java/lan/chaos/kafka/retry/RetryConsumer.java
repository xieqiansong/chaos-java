package lan.chaos.kafka.retry;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 重试消费者：消费失败触发 Spring Kafka 重试 → 耗尽后投递死信主题（DLT）。
 *
 * <p><b>Spring Kafka 错误处理演进：</b><ul>
 *   <li>2.x（SeekToCurrentErrorHandler）——seeking 当前 offset 并重试；</li>
 *   <li>2.8+（DefaultErrorHandler）——支持 {@code BackOff} 递增间隔、
 *       {@code DeadLetterPublishingRecoverer} 自动投递 DLT；</li>
 *   <li>此处依赖 {@code application.yml} 中 Spring Boot 自动配置的
 *       {@code DefaultErrorHandler} + DLT recoverer。</li>
 * </ul></p>
 *
 * <p><b>观察要点：</b>消息体含 "error" → 抛异常 → 日志打印重试次数 →
 * 重试耗尽 → 进入 demo-retry-dlt → {@link DeadLetterConsumer} 收割。</p>
 */
@Slf4j
@Component
public class RetryConsumer {

    @KafkaListener(
            topics = KafkaConstants.TOPIC_RETRY,
            groupId = KafkaConstants.GROUP_RETRY)
    public void onMessage(ConsumerRecord<String, String> record) {
        String body = record.value();
        log.info("[retry] 消费 | key={}, partition={}, offset={}, value={}",
                record.key(), record.partition(), record.offset(), body);

        // body 含 "error" → 模拟业务失败 → 触发重试 → 最终进 DLT
        if (body != null && body.contains("error")) {
            throw new RuntimeException("消费失败: " + body + "（将重试，耗尽后进 DLT）");
        }
        log.info("[retry] 消费成功 | key={}", record.key());
    }
}
