package lan.chaos.rabbitmq;

import lan.chaos.rabbitmq.common.model.OrderEvent;
import lan.chaos.rabbitmq.dlx.DeadLetterCollector;
import lan.chaos.rabbitmq.dlx.DeadLetterDemo;
import lan.chaos.rabbitmq.dlx.DelayedMessageCollector;
import lan.chaos.rabbitmq.dlx.DelayedMessageDemo;
import lan.chaos.rabbitmq.reliability.AckCollector;
import lan.chaos.rabbitmq.reliability.ConsumerAckDemo;
import lan.chaos.rabbitmq.reliability.PublisherConfirmDemo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * RabbitMQ 集成测试：Testcontainers 拉起真实 Broker，验证需真实语义的场景。
 *
 * <p><b>运行策略（与仓库 {@code jdk8-elasticsearch-demo} 一致，集成测试即 *Test，随 {@code mvn test} 运行）：</b></p>
 * <ul>
 *   <li>本机有 Docker：自动拉起 rabbitmq:3.12-management，测试真实执行；</li>
 *   <li>无 Docker：{@code @BeforeAll} 中 {@code assumeTrue} 使全部用例优雅跳过，不报错（{@code mvn test} 即绿）。</li>
 * </ul>
 *
 * <p>覆盖：生产者确认（Publisher Confirm）、消费者手动 Ack + nack 重入队、
 * TTL+DLX 死信、TTL+DLX 实现的延迟消息。</p>
 */
@SpringBootTest(classes = RabbitmqApplication.class)
class RabbitmqBrokerTest {

    private static final boolean DOCKER_AVAILABLE = DockerClientFactory.instance().isDockerAvailable();

    /** Docker 可用时启动；否则保持 null，测试经 assumeTrue 跳过 */
    static RabbitMQContainer rabbit;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            rabbit = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management"));
            rabbit.start();
            registry.add("spring.rabbitmq.host", rabbit::getHost);
            registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
            registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
            registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        }
    }

    @AfterAll
    static void stopContainer() {
        if (rabbit != null) {
            rabbit.stop();
        }
    }

    @BeforeAll
    static void guard() {
        assumeTrue(DOCKER_AVAILABLE, "本机无 Docker，跳过 RabbitMQ 集成测试");
    }

    @Autowired private PublisherConfirmDemo confirmDemo;
    @Autowired private ConsumerAckDemo ackDemo;
    @Autowired private DeadLetterDemo dlxDemo;
    @Autowired private DelayedMessageDemo delayedDemo;

    @Autowired private AckCollector ackCollector;
    @Autowired private DeadLetterCollector dlxCollector;
    @Autowired private DelayedMessageCollector delayedCollector;

    // ===================== 生产者确认 =====================

    @Test
    void confirm_shouldAckToBroker() {
        boolean confirmed = confirmDemo.publishWithConfirm(OrderEvent.sample("C1"));
        assertThat(confirmed).isTrue();
    }

    // ===================== 消费者手动 Ack + nack 重入队 =====================

    @Test
    void consumerAck_shouldAckAfterRequeue() {
        ackCollector.reset();
        ackDemo.publish(OrderEvent.sample("A1"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(ackCollector.getAcked().get()).isTrue();
            assertThat(ackCollector.getReceived()).hasSize(1);
            // 首次投递 nack 重入队 → 重投后 ack，故重入队次数 >= 1
            assertThat(ackCollector.getRequeueCount().get()).isGreaterThanOrEqualTo(1);
        });
    }

    // ===================== TTL + DLX 死信 =====================

    @Test
    void dlx_shouldDeadLetterAfterTtl() {
        dlxCollector.reset();
        dlxDemo.publish(OrderEvent.sample("D1"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(dlxCollector.getReceived())
                        .anyMatch(e -> "D1".equals(e.getOrderId())));
    }

    // ===================== 延迟消息（TTL 队列 + DLX） =====================

    @Test
    void delayed_shouldDeliverAfterDelay() {
        delayedCollector.reset();
        long start = System.currentTimeMillis();
        delayedDemo.publish(OrderEvent.sample("Y1"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(delayedCollector.getReceived())
                    .anyMatch(e -> "Y1".equals(e.getOrderId()));
            // 延迟消息应至少延迟一个 TTL 时长才送达
            assertThat(System.currentTimeMillis() - start)
                    .isGreaterThanOrEqualTo(1000L);
        });
    }
}
