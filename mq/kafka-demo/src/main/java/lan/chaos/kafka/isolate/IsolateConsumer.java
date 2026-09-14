package lan.chaos.kafka.isolate;

import lan.chaos.kafka.common.constant.KafkaConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多业务域并发隔离消费者：一个监听器同时订阅多个业务域 Topic，
 * 收到消息后按「业务域（Topic 名）」路由到<b>该域专属的线程池</b>处理。
 *
 * <p><b>面试要点（对应终端感知分析平台 Q2/Q12）：</b></p>
 * <ul>
 *   <li><b>单监听器多 Topic：</b>避免「一个 Topic 一个监听器」导致容器膨胀；
 *       用 {@code topics = {A, B}} 一处订阅，由 Kafka 负责分区分配。</li>
 *   <li><b>业务域并发隔离（核心）：</b>不同域处理耗时差异巨大（如 A 域轻量、B 域重计算）。
 *       若共用一个线程池，B 域慢任务会占满线程、拖垮 A 域实时性。
 *       按域分配独立 {@code ExecutorService}，<b>慢域阻塞不影响快域</b>。</li>
 *   <li><b>隔离的证据：</b>快域消息即便在慢域消息「处理中」也被即时处理——
 *       由 {@code fastFinishAt} / {@code slowFinishAt} 时序断言证明。</li>
 * </ul>
 *
 * <p><b>生产坑：</b>监听器方法本身若直接 <code>Thread.sleep</code> 会占用 consumer 线程，
 * 导致整个监听器（含其它域）停止拉取；必须由域线程池异步执行，consumer 线程立即返回继续 poll。</p>
 */
@Slf4j
@Component
public class IsolateConsumer {

    /** 每个业务域一个独立线程池，实现域间并发隔离。 */
    private final Map<String, ExecutorService> domainExecutors = new ConcurrentHashMap<>();

    /** 快域处理完成时间戳（用于断言「慢域阻塞不拖快域」）。 */
    private volatile Instant fastFinishAt;
    /** 慢域处理完成时间戳。 */
    private volatile Instant slowFinishAt;

    private final AtomicInteger fastCount = new AtomicInteger();
    private final AtomicInteger slowCount = new AtomicInteger();

    /**
     * 单监听器订阅两个业务域 Topic；container 用 isolateContainerFactory（独立域线程池）。
     */
    @KafkaListener(
            topics = {KafkaConstants.TOPIC_ISOLATE_A, KafkaConstants.TOPIC_ISOLATE_B},
            groupId = KafkaConstants.GROUP_ISOLATE,
            containerFactory = "isolateContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        String domain = record.topic(); // 以 Topic 名作为业务域标识
        // 取（或惰性创建）该域专属线程池，实现域间隔离
        ExecutorService executor = domainExecutors.computeIfAbsent(
                domain, d -> Executors.newFixedThreadPool(2, r -> {
                    Thread t = new Thread(r, "domain-" + d);
                    t.setDaemon(true);
                    return t;
                }));

        // 关键：提交到域线程池异步执行，consumer 线程立即返回继续拉取，
        // 不会被某域的慢处理阻塞
        executor.submit(() -> handleInDomain(domain, record));
    }

    private void handleInDomain(String domain, ConsumerRecord<String, String> record) {
        Instant start = Instant.now();
        if (KafkaConstants.TOPIC_ISOLATE_B.equals(domain)) {
            // 慢域：模拟重计算（如批量聚合/外呼调度），故意耗时
            sleep(Duration.ofMillis(800));
            slowCount.incrementAndGet();
            slowFinishAt = Instant.now();
            log.info("[isolate] 慢域处理完成 | key={}, cost={}ms, value={}",
                    record.key(), Duration.between(start, Instant.now()).toMillis(), record.value());
        } else {
            // 快域：轻量处理（如指标入库）
            sleep(Duration.ofMillis(50));
            fastCount.incrementAndGet();
            fastFinishAt = Instant.now();
            log.info("[isolate] 快域处理完成 | key={}, cost={}ms, value={}",
                    record.key(), Duration.between(start, Instant.now()).toMillis(), record.value());
        }
    }

    private void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===== 测试可观察状态 =====

    public void clear() {
        fastFinishAt = null;
        slowFinishAt = null;
        fastCount.set(0);
        slowCount.set(0);
    }

    public int getFastCount() { return fastCount.get(); }
    public int getSlowCount() { return slowCount.get(); }

    /** 快域是否先于/不晚于慢域完成（证明未被慢域阻塞）。 */
    public boolean isFastFinishedBeforeOrWithSlow() {
        if (fastFinishAt == null || slowFinishAt == null) return false;
        return !fastFinishAt.isAfter(slowFinishAt);
    }
}
