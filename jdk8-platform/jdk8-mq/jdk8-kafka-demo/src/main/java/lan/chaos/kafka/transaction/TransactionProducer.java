package lan.chaos.kafka.transaction;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 事务生产者：演示 Kafka 的「精确一次」（exactly-once）语义。
 *
 * <p><b>Kafka 事务核心概念：</b><ul>
 *   <li>事务保证「跨分区/跨 Topic 的原子写入」——要么全部提交、要么全部回滚；</li>
 *   <li>搭配 {@code read_committed} 隔离级别，消费者只读到已提交的事务消息；
 *       未提交/中止的消息不会被消费，实现 exactly-once；</li>
 *   <li>依赖 producer 幂等（{@code enable.idempotence=true}）+ 事务 id。</li>
 * </ul></p>
 *
 * <p><b>executeInTransaction：</b>事务 KafkaTemplate 由本类内部自建
 * （不暴露为 Spring bean），避免 Spring Boot 的
 * {@code @ConditionalOnMissingBean(KafkaTemplate.class)} 跳过
 * 默认非事务 template 的创建（会导致 simple/batch/filter 等场景全变成事务化）。</p>
 *
 * <p><b>与 RocketMQ 事务消息的差异：</b><ul>
 *   <li>Kafka：生产者侧事务（多分区原子写 + 消费者仅读已提交），无服务端回查；</li>
 *   <li>RocketMQ：半消息 + 本地事务回查，强调分布式事务最终一致。</li>
 * </ul></p>
 */
@Slf4j
@Service
public class TransactionProducer {

    private final KafkaTemplate<String, String> txKafkaTemplate;

    public TransactionProducer(KafkaProperties kafkaProperties) {
        Map<String, Object> configs = new HashMap<>(
                kafkaProperties.buildProducerProperties());
        configs.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-demo-producer");
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        DefaultKafkaProducerFactory<String, String> txFactory =
                new DefaultKafkaProducerFactory<>(configs);
        txFactory.setTransactionIdPrefix("tx-demo-");
        this.txKafkaTemplate = new KafkaTemplate<>(txFactory);
    }

    /**
     * 事务内原子发送多条消息；若中途抛异常则整体回滚。
     *
     * @param shouldFail 是否故意抛异常触发回滚
     * @return "COMMITTED" 或 "ROLLED_BACK"
     */
    public String sendInTransaction(String key, String prefix, boolean shouldFail) {
        try {
            txKafkaTemplate.executeInTransaction(operations -> {
                String msg1 = prefix + "-消息1";
                operations.send(KafkaConstants.TOPIC_TRANSACTION, key, msg1);
                log.info("[tx] 事务内发送消息1 | key={}, msg={}", key, msg1);

                if (shouldFail) {
                    throw new RuntimeException("模拟业务异常，触发 Kafka 事务回滚");
                }

                String msg2 = prefix + "-消息2";
                operations.send(KafkaConstants.TOPIC_TRANSACTION, key, msg2);
                log.info("[tx] 事务内发送消息2 | key={}, msg={}", key, msg2);

                return null;
            });
        } catch (Exception e) {
            log.warn("[tx] 事务回滚 | key={}, reason={}", key, e.getMessage());
            return "ROLLED_BACK";
        }
        return "COMMITTED";
    }
}
