package lan.chaos.idempotent.core;

import lan.chaos.idempotent.common.constant.Scenario;
import lan.chaos.idempotent.common.model.BizOrder;
import lan.chaos.idempotent.common.model.IdempotencyRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 请求级幂等守卫：用客户端下发的幂等号（requestId）防「前端重复提交 / 网关超时重发」。
 *
 * WHY（含金量核心）：幂等不是「防止重复发生」，而是「重复发生了也不造成重复副作用」。
 * 在 at-least-once 语义下，重试是常态。本守卫把「首检定键」与「业务副作用」放进同一事务：
 *   - 首检成功（INSERT 唯一键命中）→ 执行真实业务写，事务提交，返回结果。
 *   - 首检失败（唯一键冲突）→ 直接返回「已处理」占位，绝不执行业务副作用。
 * 并发双发时，数据库唯一约束保证只有一个事务能 INSERT 成功，另一个拿到 DuplicateKey → 被拦下。
 *
 * 生产坑：若首检与业务写不在同一事务/锁内，存在「首检成功→业务回滚→重复请求又首检成功」
 * 的窗口。本 demo 用 {@code @Transactional} 包住整段，把窗口收敛为零。
 */
@Slf4j
@Component
public class RequestIdempotentGuard {

    private final IdempotencyStore store;
    /** 统计真实业务副作用被执行的次数（用于断言「双发仅一次」） */
    private final AtomicInteger sideEffectCount = new AtomicInteger(0);

    public RequestIdempotentGuard(IdempotencyStore store) {
        this.store = store;
    }

    public int sideEffectCount() {
        return sideEffectCount.get();
    }

    /**
     * 包裹一次写动作。requestId 相同 → 只执行一次副作用。
     *
     * @param requestId 幂等号（同一次用户操作的多份重复请求共享同一 requestId）
     * @param bizNo     业务单号（落去重表，便于排障）
     * @param action    真正要执行的业务写动作
     * @return 处理后的业务单据
     */
    @Transactional
    public BizOrder execute(String requestId, String bizNo, Supplier<BizOrder> action) {
        boolean first = store.tryMarkFirst(IdempotencyRecord.builder()
                .key(requestId)
                .scope(Scenario.REQUEST.name())
                .bizNo(bizNo)
                .createdAt(LocalDateTime.now())
                .build());

        if (!first) {
            log.info("[REQUEST] 重复请求被拦截 requestId={} → 返回首检结果，未执行副作用", requestId);
            return BizOrder.builder().bizNo(bizNo).action("DEDUP").state("ALREADY_DONE").build();
        }

        // 首检通过：执行真实副作用（此处计入执行次数）
        BizOrder result = action.get();
        sideEffectCount.incrementAndGet();
        log.info("[REQUEST] 首检通过 requestId={} → 执行副作用 bizNo={}", requestId, bizNo);
        return result;
    }
}
