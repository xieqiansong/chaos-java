package lan.chaos.mqtt.lwt;

import lan.chaos.mqtt.common.constant.MqttConstants;
import lan.chaos.mqtt.common.util.MqttClients;
import lan.chaos.mqtt.common.util.MqttCollector;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 遗嘱消息（Last Will and Testament, LWT）：客户端<b>连接时</b>向 Broker 注册一条「遗言」。
 *
 * <p><b>机制：</b>当客户端<b>异常断开</b>（网络断、进程崩溃、socket 被强行关闭，<b>不含</b>正常 DISCONNECT）时，
 * Broker 自动把这条遗嘱消息发布到指定主题，通知其他订阅者「该设备掉了」。</p>
 *
 * <p><b>典型用途：</b>物联网设备掉线告警——设备连上时注册 {@code will=offline}，一掉线，监控端立刻在状态主题上收到 offline。</p>
 *
 * <p><b>本 demo 的触发关键：</b>用 {@link MqttAsyncClient#close(boolean)} 的 {@code force=true}，
 * <b>直接掐断底层 socket 且不发送 DISCONNECT</b>——这正是「异常断开」，Broker 才会发遗嘱。
 * （正常 {@code disconnect()} 会告知 Broker 主动离开，<b>不会</b>触发遗嘱。）</p>
 */
@Slf4j
@Service
public class LastWillDemo {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    /** 模拟设备异常掉线，返回 Broker 代发的遗嘱消息内容 */
    public String crashAndExpectWill() {
        String willTopic = MqttConstants.TOPIC_WILL + "/" + UUID.randomUUID();
        MqttCollector collector = new MqttCollector(1);
        MqttClient sub = null;
        MqttAsyncClient will = null;
        try {
            // 监控端：订阅该设备的遗嘱主题
            sub = MqttClients.subscriber(brokerUrl, collector, MqttConstants.QOS_1, willTopic);

            // 设备端：连接时注册遗嘱（掉线时 Broker 代发 "offline"）
            MqttConnectOptions opts = MqttClients.defaultOptions();
            opts.setWill(willTopic, "offline".getBytes(StandardCharsets.UTF_8),
                    MqttConstants.QOS_1, false);
            will = new MqttAsyncClient(brokerUrl, MqttClient.generateClientId());
            will.connect(opts).waitForCompletion(5000);
            log.info("[lwt] 设备 clientId={} 上线（已注册遗嘱 'offline'）", will.getClientId());

            // 模拟崩溃：force close 掐断 socket，不发送 DISCONNECT -> Broker 触发遗嘱
            will.close(true);

            collector.await(10000L);
            String received = collector.lastPayload();
            log.info("[lwt] Broker 在连接异常断开后发布遗嘱消息 '{}' -> {}", received, willTopic);
            return received;
        } catch (MqttException e) {
            throw new IllegalStateException("MQTT last will failed", e);
        } finally {
            MqttClients.closeQuietly(sub);
        }
    }
}
