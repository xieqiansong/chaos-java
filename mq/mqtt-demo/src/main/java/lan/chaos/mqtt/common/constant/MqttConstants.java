package lan.chaos.mqtt.common.constant;

/**
 * MQTT 演示用命名常量：主题 / QoS 等级，杜绝魔法值。
 *
 * <p>命名约定：{@code demo/mqtt/<能力>/<子主题>}；通配符约定 {@code +}=单层、{@code #}=多层。</p>
 */
public final class MqttConstants {

    private MqttConstants() {
    }

    // ===================== 基础发布 / 订阅 =====================
    public static final String TOPIC_BASIC = "demo/mqtt/basic";

    // ===================== 通配符订阅 =====================
    /** 传感器示例根主题 */
    public static final String TOPIC_SENSOR_ROOT = "demo/sensors";
    /** 单层通配：匹配 demo/sensors/room1/temp、room2/temp，不匹配 room1/humidity */
    public static final String TOPIC_WILD_SINGLE = "demo/sensors/+/temp";
    /** 多层通配：匹配 demo/sensors/ 下任意层级 */
    public static final String TOPIC_WILD_MULTI = "demo/sensors/#";

    // ===================== QoS 等级 =====================
    public static final String TOPIC_QOS = "demo/mqtt/qos";

    // ===================== 保留消息 =====================
    public static final String TOPIC_RETAINED = "demo/mqtt/retained";

    // ===================== 遗嘱消息（LWT） =====================
    public static final String TOPIC_WILL = "demo/mqtt/will";

    // ===================== QoS 取值 =====================
    public static final int QOS_0 = 0; // 至多一次：fire-and-forget，不确认
    public static final int QOS_1 = 1; // 至少一次：PUBACK 确认，可能重复
    public static final int QOS_2 = 2; // 恰好一次：PUBREC/PUBREL/PUBCOMP 四段握手，最重
}
