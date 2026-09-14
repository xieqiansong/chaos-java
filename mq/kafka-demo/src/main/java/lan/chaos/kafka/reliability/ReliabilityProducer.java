package lan.chaos.kafka.reliability;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * 可靠性生产者：消息<b>不丢（生产侧）</b>。
 *
 * <p><b>应用层不丢的关键：</b></p>
 * <ul>
 *   <li><b>确认送达</b>：必须等 Broker 真正 ack（含 partition / offset）才算发送成功，
 *       否则视为失败并<b>重试</b>；发送即忘（fire-and-forget）会静默丢消息，绝不能用于不丢场景；</li>
 *   <li><b>重试兜底</b>：本类对发送失败做有限次重试，并回调上报最终结果；</li>
 *   <li>Broker 端的「真不丢」靠<b>多副本 + acks=all + min.insync.replicas</b>
 *       （见 README「中间件层」说明），那是集群配置，非单测代码可演示。</li>
 * </ul>
 */
@Slf4j
@Service
public class ReliabilityProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ReliabilityProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 带重试的同步发送：直到 Broker ack 成功或重试耗尽。
     *
     * @return true=Broker 已确认落盘/复制
     */
    public boolean sendWithRetry(String key, String value, int maxRetries) {
        int attempt = 0;
        while (attempt <= maxRetries) {
            try {
                SendResult<String, String> result =
                        kafkaTemplate.send(KafkaConstants.TOPIC_RELIABILITY, key, value).get();
                log.info("[reliability] 发送成功(第{}次) | key={}, partition={}, offset={}",
                        attempt + 1, key, result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                return true;
            } catch (Exception e) {
                attempt++;
                log.warn("[reliability] 发送失败(第{}次) | key={}, reason={}",
                        attempt, key, e.getMessage());
                if (attempt > maxRetries) {
                    log.error("[reliability] 重试耗尽，消息未确认 | key={}", key);
                    return false;
                }
            }
        }
        return false;
    }

    /** 异步发送 + 回调确认（高吞吐场景常用）：失败由回调记录，调用方可据此告警/补偿 */
    public void sendAsyncWithCallback(String key, String value) {
        kafkaTemplate.send(KafkaConstants.TOPIC_RELIABILITY, key, value)
                .addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                    @Override
                    public void onSuccess(SendResult<String, String> result) {
                        log.info("[reliability] 异步确认成功 | key={}, offset={}",
                                key, result.getRecordMetadata().offset());
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("[reliability] 异步发送失败(需重试/告警) | key={}", key, ex);
                    }
                });
    }
}
