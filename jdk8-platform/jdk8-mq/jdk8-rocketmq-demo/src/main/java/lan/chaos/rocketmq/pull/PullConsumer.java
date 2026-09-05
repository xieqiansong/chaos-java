package lan.chaos.rocketmq.pull;

import lan.chaos.rocketmq.common.constant.MqConstant;
import lan.chaos.rocketmq.common.model.Message;
import lan.chaos.rocketmq.common.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

/**
 * 主动拉取（LitePullConsumer）模式：与 @RocketMQMessageListener 的 push 监听相反，
 * 由调用方自己决定什么时候拉、拉多少，适合流处理/批处理/背压控制的场景。
 * <p>
 * 这里在 @PostConstruct 里启动一个长生命周期的 LitePullConsumer（从最新位点开始），
 * 由 {@link #demo()} 主动 poll；与 push 模式"Broker 推给回调"形成对照。
 */
@Slf4j
@Component
public class PullConsumer {

    private final String nameServer;
    private DefaultLitePullConsumer litePullConsumer;

    public PullConsumer(@Value("${rocketmq.name-server}") String nameServer) {
        this.nameServer = nameServer;
    }

    @PostConstruct
    public void init() throws Exception {
        litePullConsumer = new DefaultLitePullConsumer(MqConstant.GROUP_PULL);
        litePullConsumer.setNamesrvAddr(nameServer);
        litePullConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        litePullConsumer.subscribe(MqConstant.TOPIC_PULL, "*");
        litePullConsumer.start();
    }

    /** 主动拉取一次（最多等 2s），打印拉到的消息与耗时 */
    public void demo() {
        List<MessageExt> msgs = litePullConsumer.poll(2000);
        if (msgs == null || msgs.isEmpty()) {
            log.info("【主动拉取】2s 内无消息");
            return;
        }
        for (MessageExt m : msgs) {
            Message envelope = MessageUtils.unpack(new String(m.getBody()));
            log.info("【主动拉取】拉到 body={} | 耗时={}ms", envelope.getBody(), MessageUtils.costMillis(envelope));
        }
    }

    @PreDestroy
    public void destroy() {
        if (litePullConsumer != null) {
            litePullConsumer.shutdown();
        }
    }
}
