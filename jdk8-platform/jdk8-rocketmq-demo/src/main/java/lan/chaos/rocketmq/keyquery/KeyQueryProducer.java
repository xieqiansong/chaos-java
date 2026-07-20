package lan.chaos.rocketmq.keyquery;

import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 按业务 Key 检索消息：发送时设置 keys（业务键，如订单号），
 * 之后可用 {@code queryMessage(topic, key)} 在 Broker 索引里按业务键反查消息，
 * 比只能靠 msgId 实用得多（msgId 业务侧一般记不住）。
 * <p>
 * 注：按 Key 检索依赖 Broker 的索引文件（commitLog 建了 index），生产环境默认开启；
 * 若未检索到，通常是消息刚发、索引尚未就绪，或 Broker 关闭了 index（enableIndex 配置）。
 */
@Slf4j
@Service
public class KeyQueryProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-keyquery-topic";

    /** 发送并打上业务 key（如 orderId），返回发送结果 */
    public String sendWithKey(String bizKey, String body) {
        org.springframework.messaging.Message<String> msg = MessageBuilder
                .withPayload(MessageUtils.pack(body))
                .setHeader(RocketMQHeaders.KEYS, bizKey)
                .build();
        String msgId = rocketMQTemplate.syncSend(TOPIC, msg).getMsgId();
        log.info("【按Key发送】key={} | msgId={}", bizKey, msgId);
        return msgId;
    }

    /** 按业务 key 反查消息（从 Broker 索引检索） */
    public void queryByKey(String bizKey) {
        try {
            QueryResult qr = rocketMQTemplate.getProducer()
                    .queryMessage(TOPIC, bizKey, 10, 0L, System.currentTimeMillis());
            if (qr == null || qr.getMessageList() == null || qr.getMessageList().isEmpty()) {
                log.info("【按Key检索】key={} | 未检索到消息（索引未就绪或已关闭）", bizKey);
                return;
            }
            for (MessageExt m : qr.getMessageList()) {
                log.info("【按Key检索】key={} | msgId={} | keys={} | topic={}",
                        bizKey, m.getMsgId(), m.getKeys(), m.getTopic());
            }
        } catch (Exception e) {
            log.error("【按Key检索】失败 key={}", bizKey, e);
        }
    }
}
