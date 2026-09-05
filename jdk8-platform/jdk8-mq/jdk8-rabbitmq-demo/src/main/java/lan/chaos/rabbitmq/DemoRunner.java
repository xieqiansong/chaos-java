package lan.chaos.rabbitmq;

import lan.chaos.rabbitmq.common.model.OrderEvent;
import lan.chaos.rabbitmq.dlx.DeadLetterDemo;
import lan.chaos.rabbitmq.dlx.DelayedMessageDemo;
import lan.chaos.rabbitmq.exchange.DirectExchangeDemo;
import lan.chaos.rabbitmq.exchange.FanoutExchangeDemo;
import lan.chaos.rabbitmq.exchange.HeadersExchangeDemo;
import lan.chaos.rabbitmq.exchange.TopicExchangeDemo;
import lan.chaos.rabbitmq.reliability.ConsumerAckDemo;
import lan.chaos.rabbitmq.reliability.IdempotentDemo;
import lan.chaos.rabbitmq.reliability.PublisherConfirmDemo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 控制台演示入口（@Profile("!mock")：仅真实 Broker 下运行，避免污染自包含 *Test）。
 *
 * <p>应用启动后发布各场景样例消息，配合各 {@code @RabbitListener} 日志即可在终端观察
 * 「输入 → 输出」。先 {@code docker compose up -d} 起 RabbitMQ，再 {@code mvn spring-boot:run}。</p>
 */
@Slf4j
@Component
@Profile("!mock")
@RequiredArgsConstructor
public class DemoRunner implements ApplicationRunner {

    private final DirectExchangeDemo directDemo;
    private final TopicExchangeDemo topicDemo;
    private final FanoutExchangeDemo fanoutDemo;
    private final HeadersExchangeDemo headersDemo;
    private final PublisherConfirmDemo confirmDemo;
    private final ConsumerAckDemo ackDemo;
    private final IdempotentDemo idempotentDemo;
    private final DeadLetterDemo dlxDemo;
    private final DelayedMessageDemo delayedDemo;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== RabbitMQ Demo 启动，发布各场景样例消息 ==========");
        directDemo.route(OrderEvent.sample("demo-direct"));
        topicDemo.publishOrderCreated(OrderEvent.sample("demo-order"));
        topicDemo.publishLogInfo(OrderEvent.sample("demo-log"));
        fanoutDemo.broadcast(OrderEvent.sample("demo-fanout"));
        headersDemo.publishTyped("report", OrderEvent.sample("demo-report"));
        headersDemo.publishTyped("notify", OrderEvent.sample("demo-notify"));
        confirmDemo.publishWithConfirm(OrderEvent.sample("demo-confirm"));
        ackDemo.publish(OrderEvent.sample("demo-ack"));
        idempotentDemo.publishDuplicate("demo-idempotent");
        dlxDemo.publish(OrderEvent.sample("demo-dlx"));
        delayedDemo.publish(OrderEvent.sample("demo-delayed"));
        log.info("========== 已发布；观察各 @RabbitListener 日志（DLX/延迟约 1~1.5s 后到达）==========");
    }
}
