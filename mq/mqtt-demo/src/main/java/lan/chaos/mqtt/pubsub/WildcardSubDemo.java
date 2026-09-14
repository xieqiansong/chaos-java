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
 * 通配符订阅：MQTT 主题用 {@code /} 分层，支持两种通配符。
 *
 * <p><b>机制：</b></p>
 * <ul>
 *     <li>{@code +} 单层通配：{@code demo/sensors/+/temp} 匹配 {@code demo/sensors/room1/temp}、
 *     {@code room2/temp}，<b>不匹配</b> {@code demo/sensors/room1/humidity}（层数不对）；</li>
 *     <li>{@code #} 多层通配：{@code demo/sensors/#} 匹配 {@code demo/sensors/} 下任意层级。</li>
 * </ul>
 *
 * <p>典型用途：一个「温度监控服务」订阅 {@code +/temp} 收全部温度；一个「总控面板」订阅 {@code #} 收全部遥测。</p>
 */
@Slf4j
@Service
public class WildcardSubDemo {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    /** 结果：单层 / 多层通配分别收到的条数，以及湿度是否错误地进了单层订阅 */
    public static class WildcardResult {
        public final int singleLevelMatches; // + 通配命中（应为 2：room1/temp、room2/temp）
        public final int multiLevelMatches;  // # 通配命中（应为 3：再加 room1/humidity）
        public final boolean humidityInSingle; // 应为 false：humidity 不该进 +/temp

        public WildcardResult(int singleLevelMatches, int multiLevelMatches, boolean humidityInSingle) {
            this.singleLevelMatches = singleLevelMatches;
            this.multiLevelMatches = multiLevelMatches;
            this.humidityInSingle = humidityInSingle;
        }
    }

    public WildcardResult wildcard() {
        MqttCollector single = new MqttCollector(2); // + 通配：两条 temp
        MqttCollector multi = new MqttCollector(3);   // # 通配：三条全收
        MqttClient subSingle = null;
        MqttClient subMulti = null;
        MqttClient pub = null;
        try {
            subSingle = MqttClients.subscriber(brokerUrl, single, MqttConstants.QOS_1, MqttConstants.TOPIC_WILD_SINGLE);
            subMulti = MqttClients.subscriber(brokerUrl, multi, MqttConstants.QOS_1, MqttConstants.TOPIC_WILD_MULTI);

            pub = MqttClients.publisher(brokerUrl);
            pub.publish("demo/sensors/room1/temp", msg("21.5"));   // 命中 + 与 #
            pub.publish("demo/sensors/room2/temp", msg("19.0"));   // 命中 + 与 #
            pub.publish("demo/sensors/room1/humidity", msg("60")); // 仅命中 #

            single.await(5000L);
            multi.await(5000L);

            boolean humidityInSingle = single.hasTopic("demo/sensors/room1/humidity");
            log.info("[wildcard] + 通配收到 {} 条(含 humidity={})；# 通配收到 {} 条",
                    single.count(), humidityInSingle, multi.count());
            return new WildcardResult(single.count(), multi.count(), humidityInSingle);
        } catch (MqttException e) {
            throw new IllegalStateException("MQTT wildcard sub failed", e);
        } finally {
            MqttClients.closeQuietly(pub);
            MqttClients.closeQuietly(subSingle);
            MqttClients.closeQuietly(subMulti);
        }
    }

    private static MqttMessage msg(String payload) {
        return new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
    }
}
