package lan.chaos.rocketmq.broadcast;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 配合 {@link BroadcastNoRetryConsumer} 的驱动生产者：
 * 发一条正常消息 + 一条 "fail" 消息，用于演示广播模式下 "fail" 不会被重试。
 */
@Slf4j
@Service
public class BroadcastNoRetryProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void send() {
        rocketMQTemplate.syncSend(MqConstant.TOPIC_BROADCAST_NO_RETRY, MessageUtils.pack("ok"));
        rocketMQTemplate.syncSend(MqConstant.TOPIC_BROADCAST_NO_RETRY, MessageUtils.pack("fail"));
        log.info("【广播-不重试】已发送 ok / fail 两条消息到 {}", MqConstant.TOPIC_BROADCAST_NO_RETRY);
    }
}
