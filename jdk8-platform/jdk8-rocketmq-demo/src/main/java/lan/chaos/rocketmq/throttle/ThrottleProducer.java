package lan.chaos.rocketmq.throttle;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 配合 {@link ThrottleConsumer} 的驱动生产者，
 * 一次性发一批消息，便于观察消费端并发线程 / 拉取批量的调优效果。
 */
@Slf4j
@Service
public class ThrottleProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void sendBatch(int count) {
        for (int i = 0; i < count; i++) {
            rocketMQTemplate.syncSend(MqConstant.TOPIC_THROTTLE, MessageUtils.pack("throttle-" + i));
        }
        log.info("【限流调优】已发送 {} 条消息到 {}", count, MqConstant.TOPIC_THROTTLE);
    }
}
