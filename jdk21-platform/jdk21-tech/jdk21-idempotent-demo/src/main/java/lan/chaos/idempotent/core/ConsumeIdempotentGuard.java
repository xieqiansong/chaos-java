package lan.chaos.idempotent.core;

import lan.chaos.idempotent.common.constant.Scenario;
import lan.chaos.idempotent.common.model.IdempotencyRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 消费级幂等守卫：用 MQ 下发的 messageId 防「至少一次投递」下的重复消费。
 *
 * WHY：消息队列（如 Rocket/Kafka 非事务消息）为保证不丢，常采用 at-least-once 投递，
 * broker 在「投递后未收到 ack」时会重发同一条消息，messageId 不变。
 * 消费侧用 messageId 做去重键：已消费过则直接 ack 跳过，避免同一条业务事件被处理多次
 * （如重复扣款、重复发通知）。
 *
 * 与请求级区别：消费侧不返回业务结果，只保证「处理动作只跑一次」；去重键来自 broker 而非客户端。
 */
@Slf4j
@Component
public class ConsumeIdempotentGuard {

    private final IdempotencyStore store;
    private final AtomicInteger consumedCount = new AtomicInteger(0);

    public ConsumeIdempotentGuard(IdempotencyStore store) {
        this.store = store;
    }

    public int consumedCount() {
        return consumedCount.get();
    }

    /**
     * 包裹一次消费动作。同 messageId 只消费一次。
     */
    public void consume(String messageId, String bizNo, Consumer<String> action) {
        boolean first = store.tryMarkFirst(IdempotencyRecord.builder()
                .key(messageId)
                .scope(Scenario.CONSUME.name())
                .bizNo(bizNo)
                .createdAt(LocalDateTime.now())
                .build());

        if (!first) {
            log.info("[CONSUME] 重复投递被跳过 messageId={} → 已消费过", messageId);
            return;
        }
        action.accept(messageId);
        consumedCount.incrementAndGet();
        log.info("[CONSUME] 首次消费 messageId={} bizNo={}", messageId, bizNo);
    }
}
