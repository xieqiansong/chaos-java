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
import org.springframework.kafka.listener.ContainerProperties;

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
}
