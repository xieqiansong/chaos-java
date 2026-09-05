package lan.chaos.mqtt;

import lan.chaos.mqtt.lwt.LastWillDemo;
import lan.chaos.mqtt.retained.RetainedMessageDemo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * MQTT 集成测试：Testcontainers 拉起真实 mosquitto，验证需真实 Broker 语义的场景。
 *
 * <p><b>运行策略（与仓库 {@code jdk8-elasticsearch-demo} / {@code jdk8-rabbitmq-demo} 一致，
 * 集成测试即 *Test，随 mvn test 运行）：</b></p>
 * <ul>
 *     <li>本机有 Docker：自动拉起 eclipse-mosquitto，测试真实执行；</li>
 *     <li>无 Docker：{@code @BeforeAll} 中 {@code assumeTrue} 使全部用例优雅跳过，不报错（mvn test 即绿）。</li>
 * </ul>
 *
 * <p>覆盖：遗嘱消息（LWT，需异常断开语义）、保留消息（真实 Broker 复验）。</p>
 */
@SpringBootTest(classes = MqttApplication.class)
@ActiveProfiles("mock")
class MqttBrokerTest {

    private static final boolean DOCKER_AVAILABLE = DockerClientFactory.instance().isDockerAvailable();

    static GenericContainer<?> mosquitto;

    @DynamicPropertySource
    static void override(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            mosquitto = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2.0.18"))
                    .withExposedPorts(1883)
                    // 覆盖默认配置为允许匿名连接（学习演示用，生产需开启认证/ACL）
                    .withCopyFileToContainer(MountableFile.forClasspathResource("mosquitto.conf"),
                            "/mosquitto/config/mosquitto.conf");
            mosquitto.start();
            registry.add("mqtt.broker-url", () -> "tcp://" + mosquitto.getHost() + ":" + mosquitto.getMappedPort(1883));
        }
    }

    @BeforeAll
    static void guard() {
        assumeTrue(DOCKER_AVAILABLE, "本机无 Docker，跳过 MQTT 集成测试");
    }

    @AfterAll
    static void stopContainer() {
        if (mosquitto != null) {
            mosquitto.stop();
        }
    }

    @Autowired private LastWillDemo lastWillDemo;
    @Autowired private RetainedMessageDemo retainedDemo;

    @Test
    void lwt_shouldPublishWillOnCrash() {
        String will = lastWillDemo.crashAndExpectWill();
        assertThat(will).isEqualTo("offline");
    }

    @Test
    void retained_onRealBroker_shouldDeliver() {
        String payload = "real-" + System.nanoTime();
        assertThat(retainedDemo.retainedDemo(payload)).isEqualTo(payload);
    }
}
