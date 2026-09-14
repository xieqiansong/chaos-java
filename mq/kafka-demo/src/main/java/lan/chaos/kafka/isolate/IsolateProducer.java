package lan.chaos.kafka.isolate;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 多业务域隔离演示生产者：分别向快域 / 慢域 Topic 发送消息，
 * 消费端 {@link IsolateConsumer} 按 Topic 路由到独立域线程池处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IsolateProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendToFastDomain(String key, String body) {
        kafkaTemplate.send(KafkaConstants.TOPIC_ISOLATE_A, key, body);
        log.info("[isolate] 发送→快域 | key={}, value={}", key, body);
    }

    public void sendToSlowDomain(String key, String body) {
        kafkaTemplate.send(KafkaConstants.TOPIC_ISOLATE_B, key, body);
        log.info("[isolate] 发送→慢域 | key={}, value={}", key, body);
    }
}
