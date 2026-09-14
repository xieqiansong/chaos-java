package lan.chaos.mqtt.common.util;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Paho 客户端便捷工厂：屏蔽连接 / 订阅样板，Demo 只关注「发布 → 订阅 → 收到」语义。
 *
 * <p>每个调用都生成独立 clientId（{@link MqttClient#generateClientId()}）并 {@code cleanSession=true}，
 * 保证场景之间互不串台；演示结束统一 {@link #closeQuietly(MqttClient)} 释放。</p>
 */
public final class MqttClients {

    private MqttClients() {
    }

    /** 通用连接选项：cleanSession=true（演示每次独立会话），短超时避免空等 */
    public static MqttConnectOptions defaultOptions() {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setConnectionTimeout(5);
        opts.setKeepAliveInterval(10);
        return opts;
    }

    /** 创建并连接一个订阅者，按 topic 绑定回调收集器 */
    public static MqttClient subscriber(String brokerUrl, MqttCollector collector, int qos, String... topics)
            throws MqttException {
        MqttClient client = new MqttClient(brokerUrl, MqttClient.generateClientId());
        client.setCallback(collector);
        client.connect(defaultOptions());
        for (String t : topics) {
            client.subscribe(t, qos);
        }
        return client;
    }

    /** 创建并连接一个发布者 */
    public static MqttClient publisher(String brokerUrl) throws MqttException {
        MqttClient client = new MqttClient(brokerUrl, MqttClient.generateClientId());
        client.connect(defaultOptions());
        return client;
    }

    /** 安静关闭：先优雅断开再 close（注意：遗嘱场景不要用它，否则会取消遗嘱） */
    public static void closeQuietly(MqttClient client) {
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception ignore) {
            // 忽略断开异常
        }
        try {
            client.close();
        } catch (Exception ignore) {
            // 忽略关闭异常
        }
    }
}
