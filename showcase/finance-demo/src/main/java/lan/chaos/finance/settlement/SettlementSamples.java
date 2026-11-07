package lan.chaos.finance.settlement;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Position;

import java.util.Arrays;
import java.util.List;

/** 清算场景样例数据工厂。 */
public final class SettlementSamples {

    private SettlementSamples() {
    }

    public static Position samplePosition(String institution, String amount) {
        Position p = new Position();
        p.setInstitution(institution);
        p.setAvailable(Money.of(amount, Currency.CNY));
        p.setFrozen(Money.of("0.00", Currency.CNY));
        return p;
    }

    public static Money zero() {
        return Money.of("0.00", Currency.CNY);
    }

    /** 双向往来：A→B 100，A→B 50，B→A 80 → 净额 A→B 70（全额 230）。 */
    public static List<NettingService.Instruction> sampleInstructions() {
        return Arrays.asList(
                new NettingService.Instruction("A", "B", Money.of("100.00", Currency.CNY)),
                new NettingService.Instruction("A", "B", Money.of("50.00", Currency.CNY)),
                new NettingService.Instruction("B", "A", Money.of("80.00", Currency.CNY)));
    }
}
