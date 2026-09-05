package lan.chaos.kafka.common.constant;

/**
 * Kafka Topic / Group 命名常量。
 *
 * <p>前缀统一为 {@code demo-}，避免与真实业务 Topic 混淆；
 * Topic 由 {@code KafkaConfig} 中的 {@code KafkaAdmin} 自动创建，
 * 无需手动 {@code kafka-topics.sh}。</p>
 */
public final class KafkaConstants {

    private KafkaConstants() {}

    // ==================== Topic ====================
    public static final String TOPIC_SIMPLE      = "demo-simple-topic";
    public static final String TOPIC_BATCH       = "demo-batch-topic";
    public static final String TOPIC_ORDER       = "demo-order-topic";
    public static final String TOPIC_TRANSACTION = "demo-tx-topic";
    public static final String TOPIC_RETRY       = "demo-retry-topic";
    public static final String TOPIC_RETRY_DLT   = "demo-retry-dlt";       // 死信主题
    public static final String TOPIC_FILTER      = "demo-filter-topic";

    // 多业务域并发隔离：单监听器绑多 Topic，按业务域路由到独立线程池
    public static final String TOPIC_ISOLATE_A    = "demo-isolate-a-topic";   // 快域（轻处理）
    public static final String TOPIC_ISOLATE_B    = "demo-isolate-b-topic";   // 慢域（重处理，易拖累）

    // ==================== Group ====================
    public static final String GROUP_SIMPLE      = "demo-simple-group";
    public static final String GROUP_BATCH       = "demo-batch-group";
    public static final String GROUP_ORDER       = "demo-order-group";
    public static final String GROUP_TRANSACTION = "demo-tx-group";
    public static final String GROUP_RETRY       = "demo-retry-group";
    public static final String GROUP_FILTER      = "demo-filter-group";
    public static final String GROUP_ISOLATE      = "demo-isolate-group";
}
