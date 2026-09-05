package lan.chaos.rocketmq.throttle;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费限流 / 并发调优演示：通过 @RocketMQMessageListener 暴露的参数控制消费端吞吐与背压。
 * <p>
 * rocketmq-spring 2.2.3 注解里可直接用的核心调优项：
 * <ul>
 *     <li>consumeThreadNumber：消费并发线程数（线程池大小，并发消费的核心手）；</li>
 *     <li>maxReconsumeTimes：并发模式下的最大重试次数（-1 表示 16 次，到达后进死信）；</li>
 *     <li>consumeTimeout：单条消息消费超时（分钟）；</li>
 *     <li>suspendCurrentQueueTimeMillis：顺序模式下挂起当前队列的间隔。</li>
 * </ul>
 * 配合生产者压一批消息即可观察并发线程数与处理规律。
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstant.TOPIC_THROTTLE,
        consumerGroup = MqConstant.GROUP_THROTTLE,
        consumeThreadNumber = 4,
        maxReconsumeTimes = 3,
        consumeTimeout = 1L)
public class ThrottleConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String payload) {
        Message envelope = MessageUtils.unpack(payload);
        // 故意 sleep 模拟耗时处理，便于观察并发线程数与积压
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        log.info("【限流调优】消费 body={} | 耗时={}ms | thread={}",
                envelope.getBody(), MessageUtils.costMillis(envelope), Thread.currentThread().getName());
    }
}
