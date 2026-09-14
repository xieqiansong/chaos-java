package lan.chaos.demo.seckill.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 配置
 */
@Configuration
public class KafkaConfig {

    /**
     * 秒杀订单 Topic
     * 分区数根据并发量设置，建议 = 消费者线程数
     */
    @Bean
    public NewTopic seckillOrderTopic() {
        return TopicBuilder.name("seckill-order")
                .partitions(8)
                .replicas(1)
                .build();
    }
}
