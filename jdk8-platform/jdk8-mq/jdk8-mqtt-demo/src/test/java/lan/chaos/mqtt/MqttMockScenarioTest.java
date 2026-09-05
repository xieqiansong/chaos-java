package lan.chaos.mqtt;

import io.moquette.BrokerConstants;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import lan.chaos.mqtt.common.model.SensorReading;
import lan.chaos.mqtt.lwt.LastWillDemo;
import lan.chaos.mqtt.pubsub.BasicPubSubDemo;
import lan.chaos.mqtt.pubsub.WildcardSubDemo;
import lan.chaos.mqtt.qos.QosDemo;
import lan.chaos.mqtt.retained.RetainedMessageDemo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MQTT 自包含单元测试：moquette 内存 Broker，无需 Docker，{@code mvn test} 即绿。
 *
 * <p>覆盖 <b>基础发布订阅 / 通配符订阅 / QoS / 保留消息</b>。遗嘱消息（LWT）需要真实 Broker 的
 * 异常断开语义，放在 {@code MqttBrokerTest}（Testcontainers）验证。</p>
 *
 * <p>moquette 0.15 运行目标 Java 8，作为进程内 Broker 类比 Kafka 的 {@code @EmbeddedKafka} /
 * RabbitMQ 的 rabbitmq-mock，让及格线测试零外部依赖。</p>
 */
@SpringBootTest(classes = MqttApplication.class)
@ActiveProfiles("mock")   // 关闭 DemoRunner（@Profile("!mock")），避免启动期连 Broker 干扰
class MqttMockScenarioTest {

    static final int EMBEDDED_PORT = 21883;
    static Server broker;

    @BeforeAll
    static void startBroker() throws Exception {
        broker = new Server();
        MemoryConfig config = new MemoryConfig(new java.util.Properties());
        config.setProperty(BrokerConstants.PORT_PROPERTY_NAME, String.valueOf(EMBEDDED_PORT));
        config.setProperty(BrokerConstants.HOST_PROPERTY_NAME, "0.0.0.0");
        broker.startServer(config);
    }

    @AfterAll
    static void stopBroker() {
        if (broker != null) {
            broker.stopServer();
        }
    }

    @DynamicPropertySource
    static void overrideBrokerUrl(DynamicPropertyRegistry registry) {
        registry.add("mqtt.broker-url", () -> "tcp://localhost:" + EMBEDDED_PORT);
    }

    @Autowired private BasicPubSubDemo basicDemo;
    @Autowired private WildcardSubDemo wildcardDemo;
    @Autowired private QosDemo qosDemo;
    @Autowired private RetainedMessageDemo retainedDemo;

    @Test
    void basic_pubsub_shouldDeliver() {
        String payload = SensorReading.sample("M1").toJson();
        String received = basicDemo.roundTrip(payload);
        assertThat(received).isEqualTo(payload);
    }

    @Test
    void wildcard_shouldRouteByLevel() {
        WildcardSubDemo.WildcardResult r = wildcardDemo.wildcard();
        assertThat(r.singleLevelMatches).isEqualTo(2);
        assertThat(r.multiLevelMatches).isEqualTo(3);
        assertThat(r.humidityInSingle).isFalse();
    }

    @Test
    void qos1_shouldDeliver() {
        int n = qosDemo.qosRoundTrip("qos1-" + System.nanoTime(), 1);
        assertThat(n).isEqualTo(1);
    }

    @Test
    void qos0_shouldDeliver() {
        int n = qosDemo.qosRoundTrip("qos0-" + System.nanoTime(), 0);
        assertThat(n).isEqualTo(1);
    }

    @Test
    void retained_shouldBeDeliveredToLateSubscriber() {
        String payload = "retained-" + System.nanoTime();
        String received = retainedDemo.retainedDemo(payload);
        assertThat(received).isEqualTo(payload);
    }
}
