package lan.chaos.rocketmq.delay;

import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 延迟（定时）消息示例。RocketMQ 的招牌特性之一。
 * <p>
 * delayLevel 1~18 对应固定档位：
 * 1s,5s,10s,30s,1m,2m,3m,4m,5m,6m,7m,8m,9m,10m,20m,30m,1h,2h。
 * 不能自定义任意毫秒，只能选档位。
 */
@Slf4j
@Service
public class DelayProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-delay-topic";

    public void sendDelay(String body, int delayLevel) {
        Message<String> msg = MessageBuilder.withPayload(MessageUtils.pack(body)).build();
        rocketMQTemplate.syncSend(TOPIC, msg, 3000, delayLevel);
        log.info("延迟消息已发送 | delayLevel={}, body={}", delayLevel, body);
    }
}
