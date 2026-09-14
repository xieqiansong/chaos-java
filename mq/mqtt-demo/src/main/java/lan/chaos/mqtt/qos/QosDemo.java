package lan.chaos.mqtt.qos;

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
 * QoS（服务质量）等级：决定一条消息「至少到达几次」。
 *
 * <p><b>三个等级：</b></p>
 * <ul>
 *     <li><b>QoS 0 至多一次</b>：fire-and-forget，发布即返回，Broker 不确认，可能丢；</li>
 *     <li><b>QoS 1 至少一次</b>：Broker 回 PUBACK 确认，发布端 {@code publish()} 阻塞到确认才返回，可能重复但不丢；</li>
 *     <li><b>QoS 2 恰好一次</b>：PUBREC/PUBREL/PUBCOMP 四段握手，最重，保证不丢不重（演示以 0/1 为主）。</li>
 * </ul>
 *
 * <p><b>坑点：</b>QoS 是「发布者 → Broker → 订阅者」两段独立协商的——取<b>两者较小值</b>生效。
 * 即发布 QoS2、订阅 QoS0，最终订阅者只按 QoS0 收到（可能丢）。</p>
 */
@Slf4j
@Service
public class QosDemo {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    /**
     * 以指定 QoS 发布一条消息，返回订阅者实际收到的条数（正常应为 1）。
     *
     * @param qos 0 / 1 / 2
     */
    public int qosRoundTrip(String payload, int qos) {
        // 唯一主题避免与其他场景串台
        String topic = MqttConstants.TOPIC_QOS + "/" + qos + "/" + UUID.randomUUID();
        MqttCollector collector = new MqttCollector(1);
        MqttClient sub = null;
        MqttClient pub = null;
        try {
            sub = MqttClients.subscriber(brokerUrl, collector, qos, topic);
            pub = MqttClients.publisher(brokerUrl);

            MqttMessage m = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            m.setQos(qos);
            // QoS1/2 时 publish() 会阻塞到对应确认（PUBACK / PUBCOMP）才返回
            pub.publish(topic, m);

            collector.await(5000L);
            log.info("[qos] QoS={} 发布 '{}' -> 订阅者收到 {} 条", qos, payload, collector.count());
            return collector.count();
        } catch (MqttException e) {
            throw new IllegalStateException("MQTT qos failed", e);
        } finally {
            MqttClients.closeQuietly(pub);
            MqttClients.closeQuietly(sub);
        }
    }
}
