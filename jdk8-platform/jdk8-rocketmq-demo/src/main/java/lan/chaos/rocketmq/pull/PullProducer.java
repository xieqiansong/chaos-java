package lan.chaos.rocketmq.pull;

import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 主动拉取（LitePullConsumer）模式的生产侧：先发几条消息，
 * 真正"拉"的动作在 {@code PullConsumer} 里由调用方自行控制节奏。
 */
@Slf4j
@Service
public class PullProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-pull-topic";

    public void send() {
        for (int i = 1; i <= 3; i++) {
            rocketMQTemplate.syncSend(TOPIC, MessageUtils.pack("pull-msg-" + i));
        }
        log.info("主动拉取：已发送 3 条到 {}", TOPIC);
    }
}
