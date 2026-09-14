package lan.chaos.kafka.retry;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 重试演示生产者：发送消息到 retry topic，由消费端模拟失败触发重试与死信投递。
 *
 * <p>消费端报错时 Spring Kafka 默认重试 10 次（固定间隔 0→30s 递增），
 * 耗尽后由 {@code DeadLetterPublishingRecoverer} 投递到死信主题（DLT）。</p>
 *
 * <p><b>为什么 DLT 很重要：</b>重试耗尽后消息必须被"收容"，
 * 否则 offset 永远不动、consumer 永远卡死在同一位置。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送一条可能失败的消息。
     *
     * @param key  消息 key
     * @param body 消息体；若包含 "error" 字样，消费端会抛异常触发重试 → DLT
     */
    public void send(String key, String body) {
        kafkaTemplate.send(KafkaConstants.TOPIC_RETRY, key, body);
        log.info("[retry] 已发送 | key={}, body={}", key, body);
    }
}
