package lan.chaos.rocketmq.common.constant;

/**
 * RocketMQ 主题(topic)与消费组(consumerGroup)常量中心。
 * <p>
 * 为什么集中定义：topic / group 是生产端与消费端必须严格对齐的「契约字符串」，一旦拼错
 * （大小写、连字符、漏字母）不会编译报错，但运行期会静默收不到消息或建错消费组，极难排查。
 * 把它们收口到一处，生产者和消费者都引用同一份常量，从源头杜绝魔法值和不一致。
 * <p>
 * 命名约定：{@code TOPIC_*} 为主题，{@code GROUP_*} 为消费组（个别场景如 ACL / Trace / 主动拉取
 * 的生产者也用 {@code GROUP_*} 作为生产者组）。死信主题由消费组名派生：{@code %DLQ% + group}。
 * <p>
 * 注意：这些字段都是「编译期常量」(public static final String = 字面量)，因此可直接用于
 * {@code @RocketMQMessageListener} 等注解的属性。
 */
public final class MqConstant {

    private MqConstant() {
    }

    // ====================== 主题(Topic) ======================

    public static final String TOPIC_BASIC = "demo-basic-topic";
    public static final String TOPIC_DELAY = "demo-delay-topic";
    public static final String TOPIC_BATCH = "demo-batch-topic";
    public static final String TOPIC_TX = "demo-tx-topic";
    public static final String TOPIC_TRACE = "demo-trace-topic";
    public static final String TOPIC_FILTER = "demo-filter-topic";
    public static final String TOPIC_THROTTLE = "demo-throttle-topic";
    public static final String TOPIC_FAULT = "demo-fault-topic";
    public static final String TOPIC_RETRY = "demo-retry-topic";
    public static final String TOPIC_PULL = "demo-pull-topic";
    public static final String TOPIC_RR = "demo-rr-topic";
    public static final String TOPIC_ORDER = "demo-order-topic";
    public static final String TOPIC_GLOBAL_ORDER = "demo-global-order-topic";
    public static final String TOPIC_BROADCAST = "demo-broadcast-topic";
    public static final String TOPIC_BROADCAST_NO_RETRY = "demo-broadcast-noretry-topic";
    public static final String TOPIC_KEYQUERY = "demo-keyquery-topic";
    public static final String TOPIC_ACL = "demo-acl-topic";
    public static final String TOPIC_RELIABILITY = "demo-reliability-topic";

    /** 消息轨迹上报专用主题（RocketMQ 内置，Broker 需预先存在） */
    public static final String TRACE_TOPIC = "RMQ_SYS_TRACE_TOPIC";

    // ====================== 消费组 / 生产者组(Group) ======================

    public static final String GROUP_BASIC = "demo-basic-consumer-group";
    public static final String GROUP_DELAY = "demo-delay-consumer-group";
    public static final String GROUP_BATCH = "demo-batch-consumer-group";
    public static final String GROUP_TX = "demo-tx-consumer-group";
    public static final String GROUP_TRACE = "demo-trace-group";
    public static final String GROUP_FILTER_TAG_A = "demo-filter-tagA-group";
    public static final String GROUP_FILTER_SCORE = "demo-filter-score-group";
    public static final String GROUP_THROTTLE = "demo-throttle-group";
    public static final String GROUP_RETRY = "demo-retry-consumer-group";
    public static final String GROUP_DLQ_HANDLER = "demo-dlq-handler-group";
    public static final String GROUP_PULL = "demo-pull-group";
    public static final String GROUP_RR = "demo-rr-group";
    public static final String GROUP_ORDER = "demo-order-consumer-group";
    public static final String GROUP_GLOBAL_ORDER = "demo-global-order-group";
    public static final String GROUP_BROADCAST_1 = "demo-broadcast-group-1";
    public static final String GROUP_BROADCAST_2 = "demo-broadcast-group-2";
    public static final String GROUP_BROADCAST_NO_RETRY = "demo-broadcast-noretry-group";
    public static final String GROUP_ACL = "demo-acl-group";
    public static final String GROUP_RELIABILITY = "demo-reliability-consumer-group";

    // ====================== 派生主题(Derived) ======================

    /** 死信主题：由重试消费组名派生，进入死信队列后由全新消费组消费做人工补偿 */
    public static final String DLQ_TOPIC_RETRY = "%DLQ%" + GROUP_RETRY;
}
