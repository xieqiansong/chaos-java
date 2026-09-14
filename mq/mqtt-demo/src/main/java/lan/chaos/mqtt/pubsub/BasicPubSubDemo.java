package lan.chaos.mqtt.pubsub;

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

/**
 * 基础发布 / 订阅：一个发布者向主题发消息，一个订阅者接收。
 *
 * <p><b>机制：</b>MQTT 核心是「发布者 → Broker（按主题路由）→ 订阅者」的解耦模型，
 * 生产者和消费者通过<b>主题（topic）</b>而非直接地址通信。这是与 RPC / 直接调用最本质的区别。</p>
 *
 * <p><b>验证方式：</b>订阅者先订阅，发布者发送后订阅者异步收到，{@link MqttCollector} 阻塞等到结果；
 * 方法返回收到的内容，便于测试断言「发送 = 收到」。</p>
 */
@Slf4j
@Service
public class BasicPubSubDemo {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    /** 发布一条消息并由订阅者同步收回应答，返回订阅者收到的内容 */
    public String roundTrip(String payload) {
        MqttCollector collector = new MqttCollector(1);
        MqttClient sub = null;
        MqttClient pub = null;
        try {
            sub = MqttClients.subscriber(brokerUrl, collector, MqttConstants.QOS_1, MqttConstants.TOPIC_BASIC);
            pub = MqttClients.publisher(brokerUrl);
            pub.publish(MqttConstants.TOPIC_BASIC, new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
            collector.await(5000L);
            String received = collector.lastPayload();
            log.info("[pubsub] 发布 '{}' -> 订阅者收到 '{}'", payload, received);
            return received;
        } catch (MqttException e) {
            throw new IllegalStateException("MQTT basic pub/sub failed", e);
        } finally {
            MqttClients.closeQuietly(pub);
            MqttClients.closeQuietly(sub);
        }
    }
}
