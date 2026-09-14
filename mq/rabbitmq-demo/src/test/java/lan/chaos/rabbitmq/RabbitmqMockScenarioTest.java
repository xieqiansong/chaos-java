package lan.chaos.rabbitmq;

import lan.chaos.rabbitmq.common.model.OrderEvent;
import lan.chaos.rabbitmq.exchange.DirectExchangeDemo;
import lan.chaos.rabbitmq.exchange.FanoutExchangeDemo;
import lan.chaos.rabbitmq.exchange.HeadersExchangeDemo;
import lan.chaos.rabbitmq.exchange.TopicExchangeDemo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * RabbitMQ 自包含单元测试：rabbitmq-mock 内存 Broker，无需 Docker，{@code mvn test} 即绿。
 *
 * <p>覆盖 <b>Exchange 四种类型路由（direct / topic / fanout / headers）+ 基础收发</b>。
 * 生产者确认 / 消费者手动 Ack / TTL+DLX 这类需要真实 Broker 语义的场景，放在
 * {@code RabbitmqBrokerIT}（Testcontainers）验证。</p>
 */
@SpringBootTest(classes = RabbitmqApplication.class)
@ActiveProfiles("mock")          // 关闭 @RabbitListener 收集器（避免占用消息，干扰 receive）
@Import(MockBrokerConfig.class)  // 用内存 Broker 覆盖真实 ConnectionFactory
class RabbitmqMockScenarioTest {

    @Autowired private DirectExchangeDemo directDemo;
    @Autowired private TopicExchangeDemo topicDemo;
    @Autowired private FanoutExchangeDemo fanoutDemo;
    @Autowired private HeadersExchangeDemo headersDemo;

    // ===================== Direct =====================

    @Test
    void direct_routing_shouldDeliver() {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            OrderEvent received = directDemo.route(OrderEvent.sample("D-" + System.nanoTime()));
            assertThat(received).isNotNull();
            assertThat(received.getOrderId()).startsWith("D-");
        });
    }

    // ===================== Topic =====================

    @Test
    void topic_shouldRouteByPattern() {
        String orderId = "O-" + System.nanoTime();
        String logId = "L-" + System.nanoTime();
        topicDemo.publishOrderCreated(OrderEvent.sample(orderId));
        topicDemo.publishLogInfo(OrderEvent.sample(logId));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            // orders 队列：仅订单消息（order.*），且不会收到日志
            OrderEvent o = topicDemo.receiveFromOrders();
            assertThat(o).isNotNull();
            assertThat(o.getOrderId()).isEqualTo(orderId);

            // logs 队列：仅日志消息（log.*），且不会收到订单
            OrderEvent l = topicDemo.receiveFromLogs();
            assertThat(l).isNotNull();
            assertThat(l.getOrderId()).isEqualTo(logId);

            // all 队列（#）：匹配全部，至少收到一条（订单或日志）
            assertThat(topicDemo.receiveFromAll()).isNotNull();
        });
    }

    // ===================== Fanout =====================

    @Test
    void fanout_shouldBroadcastToAllBoundQueues() {
        fanoutDemo.broadcast(OrderEvent.sample("F-" + System.nanoTime()));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(fanoutDemo.receiveFromA()).isNotNull();
            assertThat(fanoutDemo.receiveFromB()).isNotNull();
        });
    }

    // ===================== Headers =====================

    @Test
    void headers_shouldRouteByHeaderType() {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String reportId = "H-report-" + System.nanoTime();
            OrderEvent r = headersDemo.publishTyped("report", OrderEvent.sample(reportId));
            assertThat(r).isNotNull();
            assertThat(r.getOrderId()).isEqualTo(reportId);
            // report 类型不应进入 notify 队列
            assertThat(headersDemo.receiveFromNotify()).isNull();
        });

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String notifyId = "H-notify-" + System.nanoTime();
            OrderEvent n = headersDemo.publishTyped("notify", OrderEvent.sample(notifyId));
            assertThat(n).isNotNull();
            assertThat(n.getOrderId()).isEqualTo(notifyId);
        });
    }
}
