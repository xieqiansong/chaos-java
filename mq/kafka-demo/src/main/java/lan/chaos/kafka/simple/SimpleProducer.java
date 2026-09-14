package lan.chaos.kafka.simple;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * 基础生产者：演示 Kafka 三种发送语义。
 *
 * <p><b>Kafka 发送语义：</b><ul>
 *   <li><b>同步（syncSend）</b>——阻塞等待 Broker ack，可靠性最高；</li>
 *   <li><b>异步（asyncSend）</b>——非阻塞，回调处理结果，适合高吞吐；</li>
 *   <li><b>发送即忘（fire-and-forget）</b>——无返回值无回调，吞吐最大但无确认。</li>
 * </ul></p>
 *
 * <p><b>幂等性：</b>application.yml 已开启 {@code enable.idempotence=true}，
 * 单分区内可防重复写入（Kafka 用 PID + sequence number 去重）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 同步发送：阻塞等待 Broker 确认后返回 {@link SendResult}（含 offset/partition）。
     * 调用方必须处理异常——Broker 不可达会直接抛。
     */
    public SendResult<String, String> sendSync(String key, String value) {
        try {
            SendResult<String, String> result =
                    kafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, key, value).get();
            log.info("[simple] 同步发送成功 | key={}, partition={}, offset={}",
                    key, result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return result;
        } catch (Exception e) {
            log.error("[simple] 同步发送失败 | key={}", key, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 异步发送：立即返回，回调中处理 ack。
     * <p>注意：Spring Kafka 默认监听回调在主线程执行会导致 consumer 线程被阻塞。
     * 高吞吐场景应配置异步线程池。</p>
     */
    public void sendAsync(String key, String value) {
        kafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, key, value)
                .addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                    @Override
                    public void onSuccess(SendResult<String, String> result) {
                        log.info("[simple] 异步发送成功 | key={}, offset={}",
                                key, result.getRecordMetadata().offset());
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("[simple] 异步发送失败 | key={}", key, ex);
                    }
                });
    }

    /**
     * 发送即忘：不等待任何确认，适用于日志、监控等可容忍丢失的场景。
     * <p>⚠ 即使 Broker 宕机也不会报错（消息静默丢失）。</p>
     */
    public void sendFireAndForget(String key, String value) {
        kafkaTemplate.send(KafkaConstants.TOPIC_SIMPLE, key, value);
        log.info("[simple] 发送即忘 | key={}", key);
    }
}
