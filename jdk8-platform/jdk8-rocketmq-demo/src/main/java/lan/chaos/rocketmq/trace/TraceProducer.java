package lan.chaos.rocketmq.trace;

import lan.chaos.rocketmq.message.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 消息轨迹（Trace）：开启后 SDK 会自动把"发送 → 存储 → 消费"各阶段的耗时与状态
 * 上报到一个专门的 trace topic。排查"消息发了但消费端没收到 / 卡在哪"几乎是必备能力。
 * <p>
 * rocketmq-client 4.9.8 的 DefaultMQProducer 没有 setEnableMsgTrace 这类 setter，
 * 只能通过构造函数开启：{@code new DefaultMQProducer(group, rpcHook, enableTrace, customizedTraceTopic)}。
 * 因此本 demo 不使用共享的 RocketMQTemplate 生产者，而是自建一个带轨迹开关的生产者。
 * <p>
 * 注意：trace topic（默认 RMQ_SYS_TRACE_TOPIC）必须已存在于 Broker，否则轨迹上报会失败
 * （不影响主消息发送，仅丢失轨迹）。
 */
@Slf4j
@Service
public class TraceProducer {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    private static final String TOPIC = "demo-trace-topic";
    private static final String GROUP = "demo-trace-group";
    private static final String TRACE_TOPIC = "RMQ_SYS_TRACE_TOPIC";

    private DefaultMQProducer producer;

    @PostConstruct
    public void init() throws Exception {
        // 第 3 个参数 enableTrace=true 开启轨迹，第 4 个参数为轨迹上报的 topic
        producer = new DefaultMQProducer(GROUP, null, true, TRACE_TOPIC);
        producer.setNamesrvAddr(nameServer);
        producer.start();
    }

    public String send(String body) {
        try {
            SendResult result = producer.send(new Message(TOPIC, MessageUtils.pack(body).getBytes()));
            log.info("【消息轨迹】发送完成 | msgId={}（可在 Broker/控制台按 trace 查看链路）", result.getMsgId());
            return result.getMsgId();
        } catch (Exception e) {
            log.error("【消息轨迹】发送失败", e);
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (producer != null) {
            producer.shutdown();
        }
    }
}
