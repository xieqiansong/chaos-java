package lan.chaos.finance.common.runner;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Position;
import lan.chaos.finance.common.store.ClearingBillStore;
import lan.chaos.finance.common.store.PositionStore;
import lan.chaos.finance.settlement.NettingService;
import lan.chaos.finance.settlement.SettlementSamples;
import lan.chaos.finance.settlement.TccClearing;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 控制台演示：TCC + 轧差（P3）。正确性由 {@code *Test} 断言。 */
@Component
@org.springframework.core.annotation.Order(4)
public class SettlementDemoRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("===== [settlement] TCC + 轧差演示（P3） =====");

        PositionStore positions = new PositionStore();
        positions.save(SettlementSamples.samplePosition("A", "1000.00"));
        positions.save(SettlementSamples.samplePosition("B", "1000.00"));
        TccClearing tcc = new TccClearing(positions, new ClearingBillStore());
        Money amount = Money.of("100.00", Currency.CNY);

        tcc.tryFreeze("C1", "A", "B", amount);
        Position a = positions.get("A");
        System.out.println("Try 后 A: 可用=" + a.getAvailable() + " 冻结=" + a.getFrozen());
        tcc.confirm("C1");
        System.out.println("Confirm 后 A=" + a.getAvailable() + " B=" + positions.get("B").getAvailable());

        NettingService.NettingResult netting =
                new NettingService().net(SettlementSamples.zero(), SettlementSamples.sampleInstructions());
        System.out.println("轧差: 全额=" + netting.getGrossTotal()
                + " 净额=" + netting.getNetTotal() + " 实际划拨=" + netting.getSettlements());
        System.out.println("===== [settlement] 演示结束 =====");
    }
}
