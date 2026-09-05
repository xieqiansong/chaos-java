package lan.chaos.rocketmq.filter;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 按 SQL92 属性过滤：只消费 score &gt; 80 的消息。
 * <p>
 * <b>前提</b>：Broker 需开启 {@code enablePropertyFilter=true}（broker.conf），否则监听启动会报错。
 * 生产者通过消息头设置属性 score（被映射为 RocketMQ 用户属性）。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_FILTER,
        consumerGroup = MqConstant.GROUP_FILTER_SCORE,
        selectorType = SelectorType.SQL92,
        selectorExpression = "score > 80")
public class SqlFilterConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        Message envelope = MessageUtils.unpack(message);
        log.info("【SQL92过滤 score>80】body={} | 耗时={}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
    }
}
