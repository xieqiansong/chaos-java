package lan.chaos.rocketmq.simple;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 基础发送示例：同步 / 异步 / 单向三种语义。
 * <p>
 * <ul>
 *     <li>消息统一封装（body + timestamp），消费端据此打印耗时；</li>
 *     <li>同步发送显式处理异常（broker 不可达会抛异常，必须感知）；</li>
 *     <li>异步回调记录 msgId，便于排查；</li>
 *     <li>消费端幂等由 {@code IdempotentConsumer} 负责（RocketMQ 为 at-least-once 投递）。</li>
 * </ul>
 */
@Slf4j
@Service
public class SimpleProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /** 同步：阻塞等待 Broker 确认，可靠性最高，务必处理异常 */
    public SendResult sendSync(String body) {
        String payload = MessageUtils.pack(body);
        try {
            return rocketMQTemplate.syncSend(MqConstant.TOPIC_BASIC, payload);
        } catch (Exception e) {
            log.error("同步发送失败: {}", body, e);
            throw e;
        }
    }

    /** 异步：不阻塞主线程，回调中处理结果 */
    public void sendAsync(String body) {
        String payload = MessageUtils.pack(body);
        rocketMQTemplate.asyncSend(MqConstant.TOPIC_BASIC, payload, new SendCallback() {
            @Override
            public void onSuccess(SendResult result) {
                log.info("异步发送成功 | msgId={}, body={}", result.getMsgId(), body);
            }

            @Override
            public void onException(Throwable e) {
                log.error("异步发送失败: {}", body, e);
            }
        });
    }

    /** 单向：只发不收，吞吐最大，适合日志等可丢场景 */
    public void sendOneWay(String body) {
        rocketMQTemplate.sendOneWay(MqConstant.TOPIC_BASIC, MessageUtils.pack(body));
    }
}
