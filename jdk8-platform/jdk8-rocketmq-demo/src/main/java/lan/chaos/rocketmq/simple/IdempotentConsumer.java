package lan.chaos.rocketmq.simple;

import lan.chaos.rocketmq.common.MessageIdStore;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * 基础消费示例 + 幂等。
 * <p>
 * 关键点：RocketMQ 是 at-least-once 投递，重试 / 故障切换都会造成同一消息被重复消费。
 * 这里用 Broker 保证全局唯一的 {@code msgId} 做去重，业务处理前先判断是否已处理。
 * <p>
 * 使用 {@code RocketMQListener<MessageExt>} 才能拿到消息元数据（msgId 等）；
 * 消费时解析消息信封并打印"从发送到消费"的耗时。
 * 生产环境 {@code MessageIdStore} 应替换为 Redis(SETNX+EX) 或数据库唯一键，保证重启 / 多实例仍生效。
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "demo-basic-topic", consumerGroup = "demo-basic-consumer-group")
public class IdempotentConsumer implements RocketMQListener<MessageExt> {

    @Resource
    private MessageIdStore messageIdStore;

    @Override
    public void onMessage(MessageExt message) {
        String msgId = message.getMsgId();
        if (messageIdStore.isProcessed(msgId)) {
            log.warn("⚠️ 重复消息，已跳过 | msgId={}", msgId);
            return;
        }
        Message envelope = MessageUtils.unpack(new String(message.getBody(), StandardCharsets.UTF_8));
        log.info("✅ 处理消息 | msgId={}, body={}, 耗时={}ms", msgId, envelope.getBody(), MessageUtils.costMillis(envelope));
        // 业务处理 ...
        messageIdStore.markProcessed(msgId);
    }
}
