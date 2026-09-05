package lan.chaos.rocketmq.batch;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量发送示例。
 * <p>
 * 一次网络调用发送多条同 Topic 消息，显著降低 RTT、提升吞吐。
 * 注意事项：
 * <ul>
 *     <li>同一批消息必须同一 Topic、同一 waitStoreMsgOk；</li>
 *     <li>整批总大小默认上限约 1MB（Broker 硬限 4MB），超大需分批或压缩；</li>
 *     <li>批量只是"发送侧"优化，消费侧仍逐条投递。</li>
 * </ul>
 */
@Slf4j
@Service
public class BatchProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public SendResult sendBatch() {
        List<Message<String>> batch = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            batch.add(MessageBuilder.withPayload(MessageUtils.pack("批量消息-" + i)).build());
        }
        SendResult r = rocketMQTemplate.syncSend(MqConstant.TOPIC_BATCH, batch);
        log.info("批量发送完成 | msgId={}, 条数={}", r.getMsgId(), batch.size());
        return r;
    }
}
