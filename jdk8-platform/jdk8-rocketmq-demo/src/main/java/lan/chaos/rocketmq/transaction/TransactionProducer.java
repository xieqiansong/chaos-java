package lan.chaos.rocketmq.transaction;

import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 事务消息生产者：先发"半消息"，再执行本地事务，最后由本地事务结果决定提交/回滚。
 * 消息正文统一封装（时间戳 | 正文）。
 */
@Slf4j
@Service
public class TransactionProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-tx-topic";

    public void sendOrderTransactionMsg(String orderId) {
        String payload = MessageUtils.pack("订单创建: " + orderId);
        Message<String> msg = MessageBuilder
                .withPayload(payload)
                .setHeader("orderId", orderId)
                .build();
        rocketMQTemplate.sendMessageInTransaction(TOPIC, msg, orderId);
        log.info("事务半消息已发送 | orderId={}", orderId);
    }
}
