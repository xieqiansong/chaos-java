package lan.chaos.rocketmq.order;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 全局顺序（单 Queue）示例：与"分区有序"（按业务键哈希到同一 Queue）不同，
 * 这里用【固定 hashKey】把全部消息强制路由到【同一个 Queue】，配合 ORDERLY 消费，
 * 即可在 topic 级别保证严格全局有序（代价是失去并行度、吞吐受限）。
 * <p>
 * 与 {@code OrderedProducer}（分区有序）对照：分区有序只保证同一 orderId 有序，
 * 全局顺序保证整个 topic 所有消息有序。
 */
@Slf4j
@Service
public class GlobalOrderProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /** 用固定 hashKey 把 step-1..step-5 全部发到同一 Queue */
    public void send() {
        for (int i = 1; i <= 5; i++) {
            Message<String> msg = MessageBuilder.withPayload(MessageUtils.pack("step-" + i)).build();
            rocketMQTemplate.syncSendOrderly(MqConstant.TOPIC_GLOBAL_ORDER, msg, "GLOBAL");
        }
        log.info("全局顺序：已按顺序发送 step-1~step-5 到同一 Queue");
    }
}
