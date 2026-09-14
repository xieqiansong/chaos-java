package lan.chaos.kafka;

import lan.chaos.kafka.batch.BatchConsumer;
import lan.chaos.kafka.batch.BatchProducer;
import lan.chaos.kafka.common.constant.KafkaConstants;
import lan.chaos.kafka.filter.FilterConsumer;
import lan.chaos.kafka.filter.FilterProducer;
import lan.chaos.kafka.isolate.IsolateConsumer;
import lan.chaos.kafka.isolate.IsolateProducer;
import lan.chaos.kafka.order.OrderConsumer;
import lan.chaos.kafka.order.OrderProducer;
import lan.chaos.kafka.reliability.ReliabilityConsumer;
import lan.chaos.kafka.reliability.ReliabilityProducer;
import lan.chaos.kafka.retry.DeadLetterConsumer;
import lan.chaos.kafka.retry.RetryConsumer;
import lan.chaos.kafka.retry.RetryProducer;
import lan.chaos.kafka.simple.SimpleConsumer;
import lan.chaos.kafka.simple.SimpleProducer;
import lan.chaos.kafka.transaction.TransactionConsumer;
import lan.chaos.kafka.transaction.TransactionProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Kafka 演示统一测试入口。
 *
 * <p>使用 {@code @EmbeddedKafka} 启动内存 Kafka Broker，无需任何外部组件。
 * 克隆即跑、CI 自包含。每个 {@code @Test} 演示一个场景，独立验证可观察输出。</p>
 *
 * <p><b>对比 RocketMQ demo：</b>没有用 {@code @Disabled} 全禁再用
 * 外部 broker。EmbeddedKafka 本身就是自包含的，无需外部依赖。</p>
 */
@SpringBootTest(classes = KafkaApplication.class)
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaConstants.TOPIC_SIMPLE,
                KafkaConstants.TOPIC_BATCH,
                KafkaConstants.TOPIC_ORDER,
                KafkaConstants.TOPIC_TRANSACTION,
                KafkaConstants.TOPIC_RETRY,
                KafkaConstants.TOPIC_RETRY_DLT,
                KafkaConstants.TOPIC_FILTER,
                KafkaConstants.TOPIC_ISOLATE_A,
                KafkaConstants.TOPIC_ISOLATE_B,
                KafkaConstants.TOPIC_RELIABILITY
        },
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1",
                "offsets.topic.replication.factor=1"
        })
class KafkaScenarioTest {

    @Autowired private SimpleProducer simpleProducer;
    @Autowired private SimpleConsumer simpleConsumer;

    @Autowired private BatchProducer batchProducer;
    @Autowired private BatchConsumer batchConsumer;

    @Autowired private OrderProducer orderProducer;
    @Autowired private OrderConsumer orderConsumer;

    @Autowired private TransactionProducer transactionProducer;
    @Autowired private TransactionConsumer transactionConsumer;

    @Autowired private RetryProducer retryProducer;
    @Autowired private DeadLetterConsumer deadLetterConsumer;

    @Autowired private FilterProducer filterProducer;
    @Autowired private FilterConsumer filterConsumer;

    @Autowired private IsolateProducer isolateProducer;
    @Autowired private IsolateConsumer isolateConsumer;

    @Autowired private ReliabilityProducer reliabilityProducer;
    @Autowired private ReliabilityConsumer reliabilityConsumer;

    // ==================== Simple ====================

    @Test
    void simple_syncSend_shouldArrive() {
        simpleConsumer.clear();
        String key = "k-sync-" + System.currentTimeMillis();

        simpleProducer.sendSync(key, "sync-test-msg");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertTrue(simpleConsumer.getReceived()
                        .stream().anyMatch(m -> m.contains("sync-test-msg")),
                        "同步发送的消息应该被消费"));
    }

    @Test
    void simple_asyncSend_shouldArrive() {
        simpleConsumer.clear();
        String key = "k-async-" + System.nanoTime();

        simpleProducer.sendAsync(key, "async-test-msg");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertTrue(simpleConsumer.getReceived()
                        .stream().anyMatch(m -> m.contains("async-test-msg")),
                        "异步发送的消息应该被消费"));
    }

    @Test
    void simple_fireAndForget_shouldArrive() {
        simpleConsumer.clear();
        String key = "k-ff-" + System.nanoTime();

        simpleProducer.sendFireAndForget(key, "fire-and-forget-msg");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertTrue(simpleConsumer.getReceived()
                        .stream().anyMatch(m -> m.contains("fire-and-forget-msg")),
                        "即忘发送的消息应该被消费"));
    }

    // ==================== Batch ====================

    @Test
    void batch_shouldDeliverAll() {
        batchConsumer.clear();
        List<String> msgs = Arrays.asList(
                "batch-msg-1-" + System.nanoTime(),
                "batch-msg-2-" + System.nanoTime(),
                "batch-msg-3-" + System.nanoTime(),
                "batch-msg-4-" + System.nanoTime(),
                "batch-msg-5-" + System.nanoTime()
        );

        int sent = batchProducer.sendBatch(msgs);
        assertEquals(5, sent, "5 条消息都应发送成功");

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertEquals(5, batchConsumer.getReceived().size(),
                        "BatchConsumer 应收到全部 5 条消息"));
    }

    // ==================== Order ====================

    @Test
    void order_sameKey_shouldArriveInOrder() {
        orderConsumer.clear();
        String orderId = "ORDER-" + System.nanoTime();

        orderProducer.sendOrderLifecycle(orderId);

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<String> events = orderConsumer.getEventsFor(orderId);
                    assertEquals(4, events.size(),
                            "应有 4 个生命周期事件");
                    assertEquals(Arrays.asList("CREATE", "PAY", "SHIP", "DONE"), events,
                            "同一 orderId 的事件必须有序: CREATE → PAY → SHIP → DONE");
                });
    }

    @Test
    void order_diffKey_mayArriveInterleaved() {
        orderConsumer.clear();
        String orderA = "ORD-A-" + System.nanoTime();
        String orderB = "ORD-B-" + System.nanoTime();

        orderProducer.sendOrderLifecycle(orderA);
        orderProducer.sendOrderLifecycle(orderB);

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<String> eventsA = orderConsumer.getEventsFor(orderA);
                    List<String> eventsB = orderConsumer.getEventsFor(orderB);
                    // 各自的顺序分别独立
                    assertEquals(Arrays.asList("CREATE", "PAY", "SHIP", "DONE"), eventsA);
                    assertEquals(Arrays.asList("CREATE", "PAY", "SHIP", "DONE"), eventsB);
                });
    }

    // ==================== Transaction ====================

    @Test
    void transaction_commit_shouldDeliverBothMessages() {
        transactionConsumer.clear();
        String key = "tx-commit-" + System.nanoTime();

        String result = transactionProducer.sendInTransaction(key, "COMMIT-PREFIX", false);
        assertEquals("COMMITTED", result);

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertEquals(2, transactionConsumer.getReceived().size(),
                        "提交的事务应投递 2 条消息"));
    }

    @Test
    void transaction_rollback_shouldNotDeliver() {
        transactionConsumer.clear();
        String key = "tx-rollback-" + System.nanoTime();

        String result = transactionProducer.sendInTransaction(key, "ROLLBACK-PREFIX", true);
        assertEquals("ROLLED_BACK", result, "应该返回 ROLLED_BACK");

        // 等待足够时间确保回滚消息不被投递
        await().pollDelay(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertTrue(
                        transactionConsumer.getReceived().stream()
                                .noneMatch(m -> m.contains("ROLLBACK-PREFIX")),
                        "回滚的事务消息不应被消费"));
    }

    // ==================== Retry ====================

    @Test
    void retry_errorMessage_shouldEndUpInDLT() {
        deadLetterConsumer.clear();
        String key = "retry-dlt-" + System.nanoTime();
        String body = "boom-error-" + System.nanoTime();

        retryProducer.send(key, body);

        // RetryConsumer 消费含 "error" 的消息抛异常 → DefaultErrorHandler 重试 1 次 →
        // 耗尽后 DeadLetterPublishingRecoverer 投递到 demo-retry-dlt，由 DeadLetterConsumer 收容
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertTrue(
                        deadLetterConsumer.getDeadLetters().stream()
                                .anyMatch(m -> m.contains(body)),
                        "重试耗尽后，错误消息应被投递到 DLT（demo-retry-dlt）"));
    }

    // ==================== Filter ====================

    @Test
    void filter_onlyOrderType_shouldBeConsumed() {
        filterConsumer.clear();

        filterProducer.sendWithHeader(
                "order-1", "订单消息-" + System.nanoTime(), "ORDER");
        filterProducer.sendWithHeader(
                "log-1", "日志消息-" + System.nanoTime(), "LOG");
        filterProducer.sendWithHeader(
                "alert-1", "告警消息-" + System.nanoTime(), "ALERT");

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertEquals(1, filterConsumer.getReceived().size(),
                        "只有 ORDER 类型的消息应该被处理"));
    }

    // ==================== Isolate（多业务域并发隔离） ====================

    @Test
    void isolate_slowDomain_shouldNotBlockFastDomain() {
        isolateConsumer.clear();

        // 先发「慢域」消息（重计算 ~800ms），紧接着发「快域」消息（轻量 ~50ms）。
        // 若共用线程池，快域会因慢域占满而延迟；域隔离下快域应即时完成、不晚于慢域。
        String slowKey = "slow-" + System.nanoTime();
        String fastKey = "fast-" + System.nanoTime();
        isolateProducer.sendToSlowDomain(slowKey, "slow-domain-msg");
        isolateProducer.sendToFastDomain(fastKey, "fast-domain-msg");

        // 两个域都应各自处理完成
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertEquals(1, isolateConsumer.getSlowCount(), "慢域消息应被处理");
                    assertEquals(1, isolateConsumer.getFastCount(), "快域消息应被处理");
                });

        // 核心断言：快域处理完成时间不晚于慢域 —— 证明慢域阻塞未拖垮快域（域隔离生效）
        assertTrue(isolateConsumer.isFastFinishedBeforeOrWithSlow(),
                "快域处理应不晚于慢域完成，证明业务域并发隔离生效");
    }

    // ==================== Reliability（不丢） ====================

    @Test
    void reliability_producerConfirmAndConsumerManualAck_shouldNotLose() {
        reliabilityConsumer.clear();
        String key = "rel-" + System.nanoTime();
        String value = "reliability-msg-" + System.nanoTime();

        // 生产者：同步确认 + 重试，Broker ack 后才算成功
        boolean confirmed = reliabilityProducer.sendWithRetry(key, value, 3);
        assertTrue(confirmed, "Broker 应确认发送成功");

        // 消费者：业务处理完成后才手动提交 offset（不丢的核心）
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertEquals(1, reliabilityConsumer.getProcessedCount(),
                            "消费端业务处理并完成手动 ack 的消息应为 1 条");
                    assertTrue(reliabilityConsumer.getProcessed().contains(value),
                            "应收到并成功处理该消息");
                });
    }
}
