package lan.chaos.idempotent.core;

import lan.chaos.idempotent.common.constant.Scenario;
import lan.chaos.idempotent.common.model.BizOrder;
import lan.chaos.idempotent.common.model.IdempotencyRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 状态机级幂等守卫：流程回调重复到达时，已终态则忽略。
 *
 * WHY：工作流/审批流中，上游常因网络重试多次回调同一业务单的「完成」事件。
 * 若回调直接覆盖状态，可能把「已完成」的单子又触发一次下游动作（重复发券、重复归档）。
 * 状态机幂等的本质是「以终态为去重键」：单子一旦到达某终态，后续同单号回调直接忽略。
 * 这比请求/消费级更细粒度——它不依赖外部去重表，而是靠「业务状态本身不可重复迁移」天然去重。
 *
 * 演示用内存 Map 维护「单号→终态」，并同时写去重表（scope=STATE）做双保险，
 * 体现「状态机粗粒度 + 去重表细粒度」互补（见 README 进阶方向）。
 */
@Slf4j
@Component
public class StateMachineGuard {

    private final IdempotencyStore store;
    private final Map<String, String> terminalState = new ConcurrentHashMap<>();
    private final AtomicInteger appliedCount = new AtomicInteger(0);

    public StateMachineGuard(IdempotencyStore store) {
        this.store = store;
    }

    public int appliedCount() {
        return appliedCount.get();
    }

    /**
     * 处理一次业务单状态迁移回调。已处于终态则忽略。
     */
    public BizOrder apply(BizOrder order, Function<BizOrder, BizOrder> transition) {
        String existing = terminalState.get(order.getBizNo());
        if (existing != null) {
            log.info("[STATE] 单号={} 已处于终态={} → 回调忽略", order.getBizNo(), existing);
            return BizOrder.builder().bizNo(order.getBizNo()).action("IGNORED").state(existing).build();
        }

        boolean first = store.tryMarkFirst(IdempotencyRecord.builder()
                .key(order.getBizNo())
                .scope(Scenario.STATE.name())
                .bizNo(order.getBizNo())
                .createdAt(LocalDateTime.now())
                .build());
        if (!first) {
            log.info("[STATE] 单号={} 去重表已存在 → 回调忽略", order.getBizNo());
            return BizOrder.builder().bizNo(order.getBizNo()).action("IGNORED").state("UNKNOWN").build();
        }

        BizOrder result = transition.apply(order);
        terminalState.put(result.getBizNo(), result.getState());
        appliedCount.incrementAndGet();
        log.info("[STATE] 单号={} 状态迁移至={}", result.getBizNo(), result.getState());
        return result;
    }
}
