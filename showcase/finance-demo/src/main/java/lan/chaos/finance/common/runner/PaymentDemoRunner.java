package lan.chaos.finance.common.runner;

import lan.chaos.finance.common.constant.OrderStatus;
import lan.chaos.finance.common.store.PaymentOrderStore;
import lan.chaos.finance.payment.PayResult;
import lan.chaos.finance.payment.PaymentSamples;
import lan.chaos.finance.payment.PaymentService;
import lan.chaos.finance.payment.TradeStateMachine;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 控制台演示：状态机 + 幂等（P2）。仅用于把玩；正确性由 {@code *Test} 断言。
 */
@Component
@org.springframework.core.annotation.Order(3)
public class PaymentDemoRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        PaymentOrderStore store = new PaymentOrderStore();
        PaymentService service = new PaymentService(store, new TradeStateMachine());
        String bizNo = PaymentSamples.sampleBizNo();

        System.out.println("===== [payment] 状态机 + 幂等演示（P2） =====");
        PayResult first = service.pay(bizNo, PaymentSamples.sampleAmount());
        System.out.println("首次支付: " + first + "  → 完成真实扣款");
        PayResult again = service.pay(bizNo, PaymentSamples.sampleAmount());
        System.out.println("重复支付: " + again + "  → 幂等命中，未再扣款");
        System.out.println("真实扣款次数 = " + service.getChargeCount() + "（应为 1）");

        service.refund(bizNo);
        System.out.println("退款后状态 = " + store.findByBizNo(bizNo).getStatus());
        System.out.println("INIT 允许迁移到 = " + new TradeStateMachine().allowedTargets(OrderStatus.INIT));
        System.out.println("===== [payment] 演示结束 =====");
    }
}
