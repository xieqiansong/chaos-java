package lan.chaos.mqtt.retained;

import lan.chaos.mqtt.common.constant.MqttConstants;
import lan.chaos.mqtt.common.util.MqttClients;
import lan.chaos.mqtt.common.util.MqttCollector;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 保留消息（Retained Message）：Broker 为某主题<b>留存最后一条 retained 消息</b>。
 *
 * <p><b>机制：</b>普通消息是「发给当前在线订阅者即丢弃」；带 {@code retained=true} 的消息会被 Broker 长期保存，
 * <b>之后才上线 / 新订阅的客户端，订阅瞬间立即收到这条留存消息</b>——无需等下一次发布。</p>
 *
 * <p><b>典型用途：</b>设备状态「在线 / 离线」、配置快照——新订阅者一进来就能拿到「当前最新值」，不用空等。</p>
 *
 * <p><b>坑点：</b>保留消息是<b>每主题一份</b>，新发布会覆盖；要清除某主题的保留消息，发布一条<b>空载荷</b>的 retained 消息即可。</p>
 */
@Slf4j
@Service
public class RetainedMessageDemo {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    /** 发布一条保留消息，再由一个「后上线」的订阅者立即收取，返回收到的内容 */
    public String retainedDemo(String payload) {
        // 唯一主题，避免与并行场景互相覆盖
        String topic = MqttConstants.TOPIC_RETAINED + "/" + UUID.randomUUID();
        MqttClient pub = null;
        MqttClient sub = null;
        try {
            pub = MqttClients.publisher(brokerUrl);
            MqttMessage m = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            m.setQos(MqttConstants.QOS_1);
            m.setRetained(true);
            pub.publish(topic, m);
            log.info("[retained] 发布保留消息 '{}' -> topic={}", payload, topic);

            // 模拟「之后才上线」的订阅者：订阅瞬间即收到该保留消息
            MqttCollector collector = new MqttCollector(1);
            sub = MqttClients.subscriber(brokerUrl, collector, MqttConstants.QOS_1, topic);
            collector.await(5000L);
            String received = collector.lastPayload();
            log.info("[retained] 后订阅者立即收到保留消息 '{}'", received);
            return received;
        } catch (MqttException e) {
            throw new IllegalStateException("MQTT retained failed", e);
        } finally {
            // 清理该主题的保留消息（发布空载荷 retained），避免污染后续测试
            try {
                MqttClient cleaner = MqttClients.publisher(brokerUrl);
                MqttMessage empty = new MqttMessage(new byte[0]);
                empty.setRetained(true);
                cleaner.publish(topic, empty);
                MqttClients.closeQuietly(cleaner);
            } catch (Exception ignore) {
                // 清理失败不影响主流程
            }
            MqttClients.closeQuietly(pub);
            MqttClients.closeQuietly(sub);
        }
    }
}
