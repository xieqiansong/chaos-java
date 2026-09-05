package lan.chaos.mqtt;

import lan.chaos.mqtt.common.model.SensorReading;
import lan.chaos.mqtt.lwt.LastWillDemo;
import lan.chaos.mqtt.pubsub.BasicPubSubDemo;
import lan.chaos.mqtt.pubsub.WildcardSubDemo;
import lan.chaos.mqtt.qos.QosDemo;
import lan.chaos.mqtt.retained.RetainedMessageDemo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 控制台演示入口（@Profile("!mock")：仅真实 Broker 下运行，避免污染自包含 *Test）。
 *
 * <p>应用启动后依次跑各场景样例，配合日志即可在终端观察「输入 → 输出」。
 * 先 {@code docker compose up -d} 起 mosquitto，再 {@code mvn spring-boot:run}。</p>
 */
@Slf4j
@Component
@Profile("!mock")
@RequiredArgsConstructor
public class DemoRunner implements ApplicationRunner {

    private final BasicPubSubDemo basicDemo;
    private final WildcardSubDemo wildcardDemo;
    private final QosDemo qosDemo;
    private final RetainedMessageDemo retainedDemo;
    private final LastWillDemo lastWillDemo;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== MQTT Demo 启动，发布各场景样例消息 ==========");
        basicDemo.roundTrip(SensorReading.sample("demo-1").toJson());
        wildcardDemo.wildcard();
        qosDemo.qosRoundTrip("qos-1-payload", 1);
        qosDemo.qosRoundTrip("qos-0-payload", 0);
        retainedDemo.retainedDemo("retained-value");
        lastWillDemo.crashAndExpectWill();
        log.info("========== 各场景演示完毕，观察上方日志 ==========");
    }
}
