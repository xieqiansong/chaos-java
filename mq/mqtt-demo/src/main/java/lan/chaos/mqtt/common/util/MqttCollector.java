package lan.chaos.mqtt.common.util;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 可复用的 MQTT 接收收集器：订阅后自动收集到达的消息，供 Demo 与测试断言。
 *
 * <p>类比 RabbitMQ 各 Collector 的「接收即记账」。Paho 通过 {@link MqttCallbackExtended}
 * 异步回调 {@link #messageArrived}，这里用 {@link CountDownLatch} 让发布方能阻塞等到消息到达。</p>
 */
public class MqttCollector implements MqttCallbackExtended {

    /** 单条接收记录（主题 + 载荷） */
    public static class Received {
        public final String topic;
        public final String payload;

        public Received(String topic, String payload) {
            this.topic = topic;
            this.payload = payload;
        }
    }

    private final List<Received> received = Collections.synchronizedList(new ArrayList<>());
    private final CountDownLatch latch;

    public MqttCollector(int expected) {
        this.latch = new CountDownLatch(expected);
    }

    public MqttCollector() {
        this(1);
    }

    @Override
    public void connectionLost(Throwable cause) {
        // 演示场景不关心连接丢失
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        received.add(new Received(topic, new String(message.getPayload(), StandardCharsets.UTF_8)));
        latch.countDown();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // QoS1/2 发布完成回调，演示无需处理
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        // 演示无需处理
    }

    /** 阻塞等待期望数量的消息到达，超时返回 false */
    public boolean await(long timeoutMillis) {
        try {
            return latch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public List<Received> all() {
        return new ArrayList<>(received);
    }

    public int count() {
        return received.size();
    }

    public String lastPayload() {
        return received.isEmpty() ? null : received.get(received.size() - 1).payload;
    }

    public boolean hasTopic(String topic) {
        for (Received r : received) {
            if (topic.equals(r.topic)) {
                return true;
            }
        }
        return false;
    }
}
