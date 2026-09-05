package lan.chaos.rocketmq.broadcast;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 广播模式演示生产者。
 * <p>
 * 广播/集群是"消费侧"概念，生产者无差别发送，关键在于消费者的 {@code messageModel}。
 */
@Slf4j
@Service
public class BroadcastProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void send(String body) {
        rocketMQTemplate.syncSend(MqConstant.TOPIC_BROADCAST, MessageUtils.pack(body));
        log.info("广播消息已发送: {}", body);
    }
}
