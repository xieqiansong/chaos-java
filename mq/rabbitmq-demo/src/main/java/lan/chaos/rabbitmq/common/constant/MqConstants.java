package lan.chaos.rabbitmq.common.constant;

/**
 * RabbitMQ 演示用命名常量：交换机 / 队列 / 路由键，杜绝魔法值。
 *
 * <p>命名约定：{@code demo.<能力>.<组件>}。</p>
 */
public final class MqConstants {

    private MqConstants() {
    }

    // ===================== Exchange 类型 =====================
    public static final String DIRECT_EXCHANGE = "demo.direct.exchange";
    public static final String DIRECT_QUEUE = "demo.direct.queue";
    public static final String DIRECT_ROUTING = "demo.direct.routing";

    public static final String TOPIC_EXCHANGE = "demo.topic.exchange";
    public static final String TOPIC_QUEUE_ORDERS = "demo.topic.queue.orders";
    public static final String TOPIC_QUEUE_LOGS = "demo.topic.queue.logs";
    public static final String TOPIC_QUEUE_ALL = "demo.topic.queue.all";
    /** 路由键：order.* / log.* / #（全部） */
    public static final String TOPIC_RK_ORDER_PREFIX = "order.";
    public static final String TOPIC_RK_LOG_PREFIX = "log.";

    public static final String FANOUT_EXCHANGE = "demo.fanout.exchange";
    public static final String FANOUT_QUEUE_A = "demo.fanout.queue.a";
    public static final String FANOUT_QUEUE_B = "demo.fanout.queue.b";

    public static final String HEADERS_EXCHANGE = "demo.headers.exchange";
    public static final String HEADERS_QUEUE_REPORT = "demo.headers.queue.report";
    public static final String HEADERS_QUEUE_NOTIFY = "demo.headers.queue.notify";
    /** headers 匹配键 */
    public static final String HEADERS_MATCH_KEY = "type";

    // ===================== 可靠性：生产者确认 / 消费者手动 Ack =====================
    public static final String CONFIRM_EXCHANGE = "demo.confirm.exchange";
    public static final String CONFIRM_QUEUE = "demo.confirm.queue";
    public static final String CONFIRM_ROUTING = "demo.confirm.routing";

    public static final String ACK_EXCHANGE = "demo.ack.exchange";
    public static final String ACK_QUEUE = "demo.ack.queue";
    public static final String ACK_ROUTING = "demo.ack.routing";

    // ===================== 可靠性：幂等消费（不重） =====================
    public static final String IDEMPOTENT_EXCHANGE = "demo.idempotent.exchange";
    public static final String IDEMPOTENT_QUEUE = "demo.idempotent.queue";
    public static final String IDEMPOTENT_ROUTING = "demo.idempotent.routing";

    // ===================== TTL + DLX 死信与延迟消息 =====================
    /** 工作交换机（direct），消息先发到这里，再由工作队列经 TTL 死信到 DLX */
    public static final String WORK_EXCHANGE = "demo.dlx.work.exchange";
    public static final String WORK_QUEUE = "demo.dlx.work.queue";
    public static final String WORK_ROUTING = "demo.dlx.work.routing";

    /** 死信交换机 + 死信队列（DLT） */
    public static final String DLX_EXCHANGE = "demo.dlx.dlx.exchange";
    public static final String DLQ_QUEUE = "demo.dlx.dlq.queue";
    public static final String WORK_DLQ_ROUTING = "demo.dlx.dead.routing";

    /** 延迟队列（缓冲队列，到期死信到目标交换机 = 延迟投递） */
    public static final String DELAY_EXCHANGE = "demo.delay.target.exchange";
    public static final String DELAY_QUEUE = "demo.delay.buffer.queue";
    public static final String DELAY_TARGET_QUEUE = "demo.delay.target.queue";
    public static final String DELAY_TARGET_ROUTING = "demo.delay.target.routing";

    /** TTL 时长（毫秒）：演示用短值 */
    public static final int WORK_TTL_MS = 1000;
    public static final int DELAY_TTL_MS = 1500;
}
