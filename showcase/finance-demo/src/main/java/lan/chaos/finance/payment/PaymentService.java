package lan.chaos.finance.payment;

import lan.chaos.finance.common.constant.OrderStatus;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.TradeOrder;
import lan.chaos.finance.common.store.PaymentOrderStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 幂等支付服务（P2 核心，对应对话「幂等性 / 状态机 / 最终一致性前置」）。
 *
 * <p>WHY：金融系统极度厌恶重复扣款。同一 {@code bizNo} 的请求可能因网络重试、
 * 用户连点、MQ 重投而被调用多次，结果必须一致（只扣一次）。三层防护：
 * <ol>
 *   <li><b>唯一键约束</b>：{@link PaymentOrderStore#getOrCreate} 用 putIfAbsent 保证
 *       同一 bizNo 全局只有一个订单对象；</li>
 *   <li><b>状态机前置校验</b>：仅 INIT 可发起支付，非 INIT 直接幂等返回，杜绝重复扣款；</li>
 *   <li><b>对象锁串行化</b>：{@code synchronized(order)} 把并发的同一订单请求串行，
 *       保证 "检查状态 + 扣款 + 改状态" 不被交叉执行。</li>
 * </ol>
 *
 * <p>防御性思维三问对应：
 * <ul>
 *   <li>① 被并发调用两次？→ 唯一键 + 对象锁 + 状态前置，只扣一次（见并发测试）。</li>
 *   <li>② 中途失败留什么中间状态？→ charge 抛错时迁移到 FAILED，不悬挂在 PAYING。</li>
 *   <li>③ 消息重复消费？→ 同一 bizNo 二次进入即 duplicated，不再记账。</li>
 * </ul>
 *
 * <p>本 demo 的「扣款」用内存计数 + 流水清单模拟（可观察、可断言）；
 * 真实系统此处应调用 P1 的 {@code DoubleEntryEngine} 生成复式凭证。
 */
public class PaymentService {

    private final PaymentOrderStore store;
    private final TradeStateMachine stateMachine;
    private final AtomicInteger chargeCount = new AtomicInteger();
    private final List<String> chargeLog = Collections.synchronizedList(new ArrayList<>());

    /** 可选：注入一个"扣款动作"，用于注入失败以演示中途失败的中间状态处理。 */
    public interface ChargeAction {
        void charge(TradeOrder order);
    }

    private final ChargeAction chargeAction;

    public PaymentService(PaymentOrderStore store, TradeStateMachine stateMachine) {
        this(store, stateMachine, order -> {
        });
    }

    public PaymentService(PaymentOrderStore store, TradeStateMachine stateMachine, ChargeAction chargeAction) {
        this.store = store;
        this.stateMachine = stateMachine;
        this.chargeAction = chargeAction;
    }

    /**
     * 发起支付。同一 bizNo 无论调用多少次，真实扣款只发生一次。
     */
    public PayResult pay(String bizNo, Money amount) {
        TradeOrder order = store.getOrCreate(bizNo, () -> PaymentSamples.newOrder(bizNo, amount));

        synchronized (order) {
            // 幂等命中：非 INIT 状态一律直接返回既有结果，绝不再次扣款
            if (order.getStatus() != OrderStatus.INIT) {
                return new PayResult(order, true);
            }

            stateMachine.transition(order, OrderStatus.PAYING);
            try {
                chargeAction.charge(order);
                recordCharge(order);
                stateMachine.transition(order, OrderStatus.PAID);
            } catch (RuntimeException e) {
                // 中途失败：落到 FAILED 终态，避免订单悬挂在 PAYING 造成账务悬挂
                if (order.getStatus() == OrderStatus.PAYING) {
                    stateMachine.transition(order, OrderStatus.FAILED);
                }
                throw e;
            }
            return new PayResult(order, false);
        }
    }

    /** 退款：只有 PAID 才能 REFUNDED；由状态机拒绝 "待支付直跳退款" 这类非法跃迁。 */
    public TradeOrder refund(String bizNo) {
        TradeOrder order = store.findByBizNo(bizNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在: " + bizNo);
        }
        synchronized (order) {
            stateMachine.transition(order, OrderStatus.REFUNDED);
        }
        return order;
    }

    private void recordCharge(TradeOrder order) {
        chargeCount.incrementAndGet();
        chargeLog.add(order.getBizNo() + " " + order.getAmount()
                + " " + order.getAmount().getCurrency().getIsoCode());
    }

    /** 真实扣款次数（可观察、可断言）。 */
    public int getChargeCount() {
        return chargeCount.get();
    }

    public List<String> getChargeLog() {
        return Collections.unmodifiableList(new ArrayList<>(chargeLog));
    }
}
