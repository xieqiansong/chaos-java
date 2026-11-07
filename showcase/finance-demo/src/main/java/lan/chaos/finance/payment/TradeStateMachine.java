package lan.chaos.finance.payment;

import lan.chaos.finance.common.constant.OrderStatus;
import lan.chaos.finance.common.exception.IllegalStateTransitionException;
import lan.chaos.finance.common.model.TradeOrder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 交易状态机（P2 核心，对应对话「状态机 State Machine」）。
 *
 * <p>WHY：交易订单的生命周期必须由明确的迁移规则驱动。把规则<b>集中成一张白名单表</b>，
 * 而不是在业务代码里写 {@code if (status == ...)} 拼状态 —— 后者极易漏判，
 * 一旦出现"待支付直接跳退款"这类非法跃迁，钱与状态会错位，属于资金事故。
 *
 * <p>合法迁移：
 * <pre>
 *   INIT    → PAYING, FAILED
 *   PAYING  → PAID, FAILED
 *   PAID    → REFUNDED
 *   FAILED  → （终态）
 *   REFUNDED→ （终态）
 * </pre>
 *
 * <p>防御性思维：每次迁移校验「当前状态 + 目标状态」是否在白名单，非法即拒绝；
 * 同时推进 {@code version}（乐观锁），生产环境用它挡并发下的重复状态更新。
 */
public class TradeStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED;

    static {
        EnumMap<OrderStatus, Set<OrderStatus>> rules = new EnumMap<>(OrderStatus.class);
        rules.put(OrderStatus.INIT, EnumSet.of(OrderStatus.PAYING, OrderStatus.FAILED));
        rules.put(OrderStatus.PAYING, EnumSet.of(OrderStatus.PAID, OrderStatus.FAILED));
        rules.put(OrderStatus.PAID, EnumSet.of(OrderStatus.REFUNDED));
        rules.put(OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class));
        rules.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED = Collections.unmodifiableMap(rules);
    }

    /** 执行状态迁移；非法跃迁抛 {@link IllegalStateTransitionException}。 */
    public void transition(TradeOrder order, OrderStatus target) {
        OrderStatus current = order.getStatus();
        Set<OrderStatus> allowed = ALLOWED.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new IllegalStateTransitionException(
                    "非法状态跃迁: " + current + " → " + target + "（订单 " + order.getBizNo() + "）");
        }
        order.setStatus(target);
        order.setVersion(order.getVersion() + 1);
    }

    /** 返回某状态允许迁移到的目标集合（供演示/测试观察规则）。 */
    public Set<OrderStatus> allowedTargets(OrderStatus current) {
        Set<OrderStatus> set = ALLOWED.get(current);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }
}
