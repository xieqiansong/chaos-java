package lan.chaos.rocketmq.retry;

import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 重试演示生产者。
 * <p>
 * 不在应用启动时自动发消息，改为由测试按需触发，互不干扰。
 */
@Slf4j
@Service
public class RetryProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-retry-topic";

    /** 发送一条消息；body 含 "error" 时消费端会抛异常触发重试 */
    public void send(String body) {
        rocketMQTemplate.syncSend(TOPIC, MessageUtils.pack(body));
        log.info("已发送重试演示消息: {}", body);
    }
}
