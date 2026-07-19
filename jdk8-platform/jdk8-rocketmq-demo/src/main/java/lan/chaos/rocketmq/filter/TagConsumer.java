package lan.chaos.rocketmq.filter;

import lan.chaos.rocketmq.message.Message;
import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 按 Tag 过滤：只消费 TagA（默认模式，Broker 无需额外配置）。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "demo-filter-topic",
        consumerGroup = "demo-filter-tagA-group",
        selectorExpression = "TagA")
public class TagConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("【Tag过滤 TagA】body={} | 耗时={}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
    }
}
