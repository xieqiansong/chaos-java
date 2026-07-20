package lan.chaos.kafka.common.config;

import lan.chaos.kafka.common.constant.KafkaConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.function.BiFunction;
import org.apache.kafka.common.TopicPartition;

/**
 * Kafka 公共配置：自动建 Topic + 独立的批量消费工厂 + 独立的事务工厂。
 *
 * <p><b>设计取舍：</b>Demo 场景用 {@link KafkaAdmin} 自动建 Topic，
 * 避免依赖外部 {@code kafka-topics.sh}；生产环境多由运维统一管理 Topic，
 * 不应在生产代码中自动创建。</p>
 *
 * <p><b>事务隔离：</b>事务 KafkaTemplate 不在此处注册 bean——
 * 原因：Spring Boot 的 {@code @ConditionalOnMissingBean(KafkaTemplate.class)}
 * 一旦发现任何 {@code KafkaTemplate} bean 就会跳过默认非事务 template 的创建，
 * 导致 simple/batch/filter 等场景全变成事务化。
 * 事务 template 由 {@code TransactionProducer} 内部自建，不影响全局。</p>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * 自动创建 Demo 用 Topic（单分区、无副本，学习够用）。
     */
    @Bean
    public KafkaAdmin.NewTopics demoTopic() {
        return new KafkaAdmin.NewTopics(
                topic(KafkaConstants.TOPIC_SIMPLE),
                topic(KafkaConstants.TOPIC_BATCH),
                topic(KafkaConstants.TOPIC_ORDER),
                topic(KafkaConstants.TOPIC_TRANSACTION),
                topic(KafkaConstants.TOPIC_RETRY),
                topic(KafkaConstants.TOPIC_RETRY_DLT),
                topic(KafkaConstants.TOPIC_FILTER)
        );
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * 批量消费专用 ContainerFactory——仅在 {@code BatchConsumer} 中引用。
     */
    @Bean("batchContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> batchContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }

    /**
     * 重试/死信专用 ContainerFactory——仅在 {@link lan.chaos.kafka.retry.RetryConsumer} 中引用，
     * 作用域隔离，不影响 simple/batch/order 等其它场景。
     * 注入 {@code DefaultErrorHandler} + {@code DeadLetterPublishingRecoverer}：
     * 消费抛异常时按 {@link FixedBackOff} 重试，耗尽后自动投递到 {@code <topic>-dlt}（即 demo-retry-dlt）。
     */
    @Bean("retryContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> retryContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // 死信投递到 demo-retry-dlt（与 DeadLetterConsumer 订阅的常量一致），
        // 而非默认规则 <topic>.DLT（即 demo-retry-topic.DLT），否则收不到
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(KafkaConstants.TOPIC_RETRY_DLT, record.partition()));
        // 重试 1 次（共 2 次投递）后进死信；间隔 500ms，教学观察足够且不拖慢测试
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 1L));
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
