package lan.chaos.rocketmq.filter;

import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 消息过滤示例（Tag + SQL92 属性过滤）。
 * <p>
 * 生产者通过 destination 的 {@code topic:tag} 设置 Tag，并通过消息头设置用户属性（score）。
 * 消费者侧：
 * <ul>
 *     <li>{@code TagConsumer} 用 Tag 订阅（默认支持，无需 Broker 额外配置）；</li>
 *     <li>{@code SqlFilterConsumer} 用 SQL92 按属性过滤，<b>要求 Broker 开启 enablePropertyFilter=true</b>。</li>
 * </ul>
 * 过滤在 Broker 侧完成，比"全收再业务判断"更省带宽与算力。
 */
@Slf4j
@Service
public class FilterProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-filter-topic";

    public void send() {
        Message<String> a = MessageBuilder.withPayload(MessageUtils.pack("TagA消息 score=90"))
                .setHeader("score", 90).build();
        Message<String> b = MessageBuilder.withPayload(MessageUtils.pack("TagB消息 score=60"))
                .setHeader("score", 60).build();
        Message<String> c = MessageBuilder.withPayload(MessageUtils.pack("TagA消息 score=95"))
                .setHeader("score", 95).build();

        rocketMQTemplate.syncSend(TOPIC + ":TagA", a);
        rocketMQTemplate.syncSend(TOPIC + ":TagB", b);
        rocketMQTemplate.syncSend(TOPIC + ":TagA", c);
        log.info("过滤演示消息已发送（TagA×2 / TagB×1，含 score 属性）");
    }
}
