package lan.chaos.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka 演示启动类。
 *
 * <p>各 Kafka 能力场景（基础收发、批量发送、分区间有序、事务 exact-once、重试/死信、
 * 消息过滤）按能力分包于 {@code lan.chaos.kafka.<capability>}，
 * 由 {@code KafkaScenarioTest} 通过 @EmbeddedKafka 自包含测试验证。</p>
 *
 * <p>若需连真实 Kafka Broker 手动把玩：先 {@code docker compose up -d}，
 * 再 {@code mvn -pl jdk8-kafka-demo spring-boot:run} 启动应用，
 * 观察各 {@code @KafkaListener} 控制台日志。</p>
 */
@SpringBootApplication
public class KafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaApplication.class, args);
    }
}
