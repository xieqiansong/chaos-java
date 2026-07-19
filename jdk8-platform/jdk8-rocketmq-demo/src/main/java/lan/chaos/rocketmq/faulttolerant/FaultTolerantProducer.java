package lan.chaos.rocketmq.faulttolerant;

import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 发送侧容错参数：生产环境默认就该显式配置的几个"抗抖动"开关。
 * <p>
 * 这些参数属于 DefaultMQProducer 的全局配置，通常应在生产者初始化时一次性设定
 * （本 demo 在发送方法里临时设置，便于单点演示；正式代码请在 @PostConstruct / 配置类里固定）。
 * <ul>
 *     <li>retryTimesWhenSendFailed：同步发送失败后的重试次数（默认 2）；</li>
 *     <li>sendMsgTimeout：单次发送超时（ms，默认 3000）；</li>
 *     <li>sendLatencyFaultEnable：开启"避峰"，对延迟高的 Broker 短期规避，把流量切到健康的节点；</li>
 *     <li>retryAnotherBrokerWhenNotStoreOK：Broker 存储失败（落盘未 OK）时换一个 Broker 重试。</li>
 * </ul>
 * 对照：若不开启 sendLatencyFaultEnable，某个慢 Broker 会持续被选中，拖累整体发送耗时。
 */
@Slf4j
@Service
public class FaultTolerantProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-fault-topic";

    public String send(String body) {
        DefaultMQProducer producer = rocketMQTemplate.getProducer();
        producer.setRetryTimesWhenSendFailed(3);
        producer.setSendMsgTimeout(3000);
        producer.setSendLatencyFaultEnable(true);
        producer.setRetryAnotherBrokerWhenNotStoreOK(true);

        log.info("【发送容错】retry={}, timeout={}ms, latencyFault={}",
                producer.getRetryTimesWhenSendFailed(),
                producer.getSendMsgTimeout(),
                producer.isSendLatencyFaultEnable());

        SendResult result = rocketMQTemplate.syncSend(TOPIC, MessageUtils.pack(body));
        log.info("【发送容错】发送完成 | msgId={}", result.getMsgId());
        return result.getMsgId();
    }
}
