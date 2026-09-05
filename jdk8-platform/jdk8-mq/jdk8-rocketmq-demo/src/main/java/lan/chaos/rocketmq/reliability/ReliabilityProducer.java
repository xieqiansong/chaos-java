package lan.chaos.rocketmq.reliability;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 可靠性生产者：消息<b>不丢（生产侧）</b>。
 *
 * <p><b>应用层不丢的关键：</b></p>
 * <ul>
 *   <li>用<b>同步发送</b> {@code syncSend} 并校验 {@link SendStatus#SEND_OK}，
 *       只有 Broker 真正写入成功才算发送成功；异步/单向发送无法即时感知失败，需配合回调与重试；</li>
 *   <li>发送失败（网络抖动 / Broker 切换）由 <b>生产者重试</b> 兜底
 *       （{@code retryTimesWhenSendFailed}，见 application.yml）；</li>
 *   <li>设置<b>消息 Key</b> 便于按 key 查询与消费端幂等去重；</li>
 *   <li>Broker 端「真不丢」靠同步刷盘 + 主从同步 + Dledger（见 README「中间件层」说明）。</li>
 * </ul>
 */
@Slf4j
@Service
public class ReliabilityProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /** 同步发送并校验状态：返回 true 表示 Broker 已确认写入 */
    public boolean sendGuaranteed(String key, String body) {
        String payload = MessageUtils.pack(body);
        Message<String> msg = MessageBuilder.withPayload(payload)
                .setHeader("KEYS", key)
                .build();
        try {
            SendResult result = rocketMQTemplate.syncSend(MqConstant.TOPIC_RELIABILITY, msg);
            if (result.getSendStatus() == SendStatus.SEND_OK) {
                log.info("[reliability] 发送成功 | msgId={}, body={}", result.getMsgId(), body);
                return true;
            }
            log.warn("[reliability] 发送状态异常 | status={}, body={}", result.getSendStatus(), body);
            return false;
        } catch (Exception e) {
            log.error("[reliability] 发送异常(将触发生产者重试) | body={}", body, e);
            throw e;
        }
    }
}
