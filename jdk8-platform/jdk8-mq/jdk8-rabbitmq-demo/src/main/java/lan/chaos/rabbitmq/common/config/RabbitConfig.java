package lan.chaos.rabbitmq.common.config;

import lan.chaos.rabbitmq.common.constant.MqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 公共配置（◆ 基础模块，非独立业务场景）。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>JSON 消息转换器（{@link Jackson2JsonMessageConverter}）：替代默认 JDK 序列化，跨语言可读；</li>
 *     <li>统一 {@link RabbitTemplate}：开启 <b>Publisher Confirm</b>（{@code publisherConfirms=true}），供生产者确认场景使用；</li>
 *     <li>手动 Ack 的监听器容器工厂（{@code AcknowledgeMode.MANUAL}）：供 consumer-ack 场景演示 {@code basicAck/basicNack}；</li>
 *     <li>声明全部拓扑（Exchange / Queue / Binding）：开箱即跑，无需手动建组件。</li>
 * </ul>
 *
 * <p><b>DLX / 延迟拓扑仅在非 mock 环境声明</b>（{@code @Profile("!mock")}）：内存 Broker（rabbitmq-mock）
 * 对死信 / 延迟参数支持有限，只在真实 Broker（*IT / app）上才需要；自包含的 {@code *Test} 只声明
 * Exchange 路由相关拓扑，避免 mock 声明失败拖垮及格线测试。</p>
 */
@Slf4j
@Configuration
public class RabbitConfig {

    // ===================== 消息转换器 =====================

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        // 默认携带 __TypeId__，receive 时据此反序列化为 OrderEvent
        ObjectMapper mapper = new ObjectMapper();
        // 注册 JSR-310，支持 OrderEvent.createdAt（LocalDateTime）的序列化/反序列化
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    // ===================== 统一 RabbitTemplate（Publisher Confirm 由 application.yml 开启） =====================

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        // Publisher Confirm 由 application.yml 的 spring.rabbitmq.publisher-confirms=true 开启
        // （真实 Broker / *IT 生效；mock 自包含 *Test 不依赖确认语义，无需开启）
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    // ===================== 手动 Ack 监听器容器工厂 =====================

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // 手动 Ack：消费端业务处理完成后再 basicAck，配合 basicNack(requeue) 演示重试
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    // ===================== Exchange 类型路由拓扑（始终声明，*Test 也用） =====================

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(MqConstants.DIRECT_EXCHANGE);
    }

    @Bean
    public Queue directQueue() {
        return new Queue(MqConstants.DIRECT_QUEUE, true);
    }

    @Bean
    public Binding directBinding() {
        return BindingBuilder.bind(directQueue()).to(directExchange())
                .with(MqConstants.DIRECT_ROUTING);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(MqConstants.TOPIC_EXCHANGE);
    }

    @Bean
    public Queue topicOrdersQueue() {
        return new Queue(MqConstants.TOPIC_QUEUE_ORDERS, true);
    }

    @Bean
    public Queue topicLogsQueue() {
        return new Queue(MqConstants.TOPIC_QUEUE_LOGS, true);
    }

    @Bean
    public Queue topicAllQueue() {
        return new Queue(MqConstants.TOPIC_QUEUE_ALL, true);
    }

    @Bean
    public Binding topicOrdersBinding() {
        return BindingBuilder.bind(topicOrdersQueue()).to(topicExchange())
                .with(MqConstants.TOPIC_RK_ORDER_PREFIX + "#"); // order.*
    }

    @Bean
    public Binding topicLogsBinding() {
        return BindingBuilder.bind(topicLogsQueue()).to(topicExchange())
                .with(MqConstants.TOPIC_RK_LOG_PREFIX + "#"); // log.*
    }

    @Bean
    public Binding topicAllBinding() {
        return BindingBuilder.bind(topicAllQueue()).to(topicExchange()).with("#"); // 全部
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(MqConstants.FANOUT_EXCHANGE);
    }

    @Bean
    public Queue fanoutQueueA() {
        return new Queue(MqConstants.FANOUT_QUEUE_A, true);
    }

    @Bean
    public Queue fanoutQueueB() {
        return new Queue(MqConstants.FANOUT_QUEUE_B, true);
    }

    @Bean
    public Binding fanoutBindingA() {
        return BindingBuilder.bind(fanoutQueueA()).to(fanoutExchange());
    }

    @Bean
    public Binding fanoutBindingB() {
        return BindingBuilder.bind(fanoutQueueB()).to(fanoutExchange());
    }

    @Bean
    public HeadersExchange headersExchange() {
        return new HeadersExchange(MqConstants.HEADERS_EXCHANGE);
    }

    @Bean
    public Queue headersReportQueue() {
        return new Queue(MqConstants.HEADERS_QUEUE_REPORT, true);
    }

    @Bean
    public Queue headersNotifyQueue() {
        return new Queue(MqConstants.HEADERS_QUEUE_NOTIFY, true);
    }

    @Bean
    public Binding headersReportBinding() {
        // 仅当消息 header 中 type=report 时路由到 report 队列
        return BindingBuilder.bind(headersReportQueue()).to(headersExchange())
                .where(MqConstants.HEADERS_MATCH_KEY).matches("report");
    }

    @Bean
    public Binding headersNotifyBinding() {
        // 仅当消息 header 中 type=notify 时路由到 notify 队列
        return BindingBuilder.bind(headersNotifyQueue()).to(headersExchange())
                .where(MqConstants.HEADERS_MATCH_KEY).matches("notify");
    }

    // ===================== 可靠性拓扑（始终声明，*Test 不用但声明无副作用） =====================

    @Bean
    public DirectExchange confirmExchange() {
        return new DirectExchange(MqConstants.CONFIRM_EXCHANGE);
    }

    @Bean
    public Queue confirmQueue() {
        return new Queue(MqConstants.CONFIRM_QUEUE, true);
    }

    @Bean
    public Binding confirmBinding() {
        return BindingBuilder.bind(confirmQueue()).to(confirmExchange())
                .with(MqConstants.CONFIRM_ROUTING);
    }

    @Bean
    public DirectExchange ackExchange() {
        return new DirectExchange(MqConstants.ACK_EXCHANGE);
    }

    @Bean
    public Queue ackQueue() {
        return new Queue(MqConstants.ACK_QUEUE, true);
    }

    @Bean
    public Binding ackBinding() {
        return BindingBuilder.bind(ackQueue()).to(ackExchange()).with(MqConstants.ACK_ROUTING);
    }

    // ===================== 可靠性：幂等消费（不重）拓扑 =====================

    @Bean
    public DirectExchange idempotentExchange() {
        return new DirectExchange(MqConstants.IDEMPOTENT_EXCHANGE);
    }

    @Bean
    public Queue idempotentQueue() {
        return new Queue(MqConstants.IDEMPOTENT_QUEUE, true);
    }

    @Bean
    public Binding idempotentBinding() {
        return BindingBuilder.bind(idempotentQueue()).to(idempotentExchange())
                .with(MqConstants.IDEMPOTENT_ROUTING);
    }

    // ===================== TTL + DLX 死信 / 延迟拓扑（仅真实 Broker：非 mock） =====================

    @Bean
    @Profile("!mock")
    public DirectExchange workExchange() {
        return new DirectExchange(MqConstants.WORK_EXCHANGE);
    }

    @Bean
    @Profile("!mock")
    public Queue workQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", MqConstants.WORK_TTL_MS);                 // 消息存活时间
        args.put("x-dead-letter-exchange", MqConstants.DLX_EXCHANGE);      // 到期后死信到 DLX
        args.put("x-dead-letter-routing-key", MqConstants.WORK_DLQ_ROUTING);
        return new Queue(MqConstants.WORK_QUEUE, true, false, false, args);
    }

    @Bean
    @Profile("!mock")
    public Binding workBinding() {
        return BindingBuilder.bind(workQueue()).to(workExchange()).with(MqConstants.WORK_ROUTING);
    }

    @Bean
    @Profile("!mock")
    public DirectExchange dlxExchange() {
        return new DirectExchange(MqConstants.DLX_EXCHANGE);
    }

    @Bean
    @Profile("!mock")
    public Queue dlqQueue() {
        return new Queue(MqConstants.DLQ_QUEUE, true);
    }

    @Bean
    @Profile("!mock")
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with(MqConstants.WORK_DLQ_ROUTING);
    }

    @Bean
    @Profile("!mock")
    public DirectExchange delayExchange() {
        return new DirectExchange(MqConstants.DELAY_EXCHANGE);
    }

    @Bean
    @Profile("!mock")
    public Queue delayBufferQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", MqConstants.DELAY_TTL_MS);                  // 缓冲时长 = 延迟时长
        args.put("x-dead-letter-exchange", MqConstants.DELAY_EXCHANGE);      // 到期死信到目标交换机
        args.put("x-dead-letter-routing-key", MqConstants.DELAY_TARGET_ROUTING);
        return new Queue(MqConstants.DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    @Profile("!mock")
    public Queue delayTargetQueue() {
        return new Queue(MqConstants.DELAY_TARGET_QUEUE, true);
    }

    @Bean
    @Profile("!mock")
    public Binding delayTargetBinding() {
        return BindingBuilder.bind(delayTargetQueue()).to(delayExchange())
                .with(MqConstants.DELAY_TARGET_ROUTING);
    }
}
