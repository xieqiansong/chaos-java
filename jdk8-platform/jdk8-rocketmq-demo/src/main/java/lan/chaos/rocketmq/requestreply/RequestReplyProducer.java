package lan.chaos.rocketmq.requestreply;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 请求-应答（Request-Reply）示例：生产者发消息后阻塞等待消费者回传结果，
 * 把 MQ 当成异步 RPC 使用。适用于"发指令并关心返回"的场景（如任务下发+结果回收）。
 * <p>
 * 底层：RocketMQ 4.6+ 原生支持，reply 通过自动创建的 reply topic 回传。
 * 对应消费者见 {@code RequestReplyConsumer}（实现 RocketMQReplyListener）。
 */
@Slf4j
@Service
public class RequestReplyProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final String TOPIC = "demo-rr-topic";

    /** 发送请求并等待回复（超时 5s） */
    public void send() {
        String body = "request-" + System.currentTimeMillis();
        String reply = rocketMQTemplate.sendAndReceive(TOPIC, body, String.class, 5000);
        log.info("【请求应答】发送={} | 收到回复={}", body, reply);
    }
}
