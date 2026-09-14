package lan.chaos.idempotent.demo;

import lan.chaos.idempotent.common.model.BizOrder;
import lan.chaos.idempotent.core.StateMachineGuard;

/**
 * 场景三：状态机级重复回调。
 * 模拟上游对同一业务单两次回调「完成」事件。
 * 期望：首次回调推进到终态，二次回调被忽略（不脏写）。
 */
public class StateMachineDedupDemo {

    private final StateMachineGuard guard;

    public StateMachineDedupDemo(StateMachineGuard guard) {
        this.guard = guard;
    }

    public String run() {
        BizOrder order = BizOrder.builder().bizNo("BIZ-DEMO-1").action("CONFIRM").state("CONFIRMED").build();
        // 第一次回调
        BizOrder r1 = guard.apply(order, o -> BizOrder.builder()
                .bizNo(o.getBizNo()).action(o.getAction()).state(o.getState()).build());
        // 重复回调
        BizOrder r2 = guard.apply(order, o -> BizOrder.builder()
                .bizNo(o.getBizNo()).action(o.getAction()).state(o.getState()).build());
        return "单号=" + order.getBizNo()
                + " 首次=" + r1.getState()
                + " 重复回调=" + r2.getAction();
    }
}
