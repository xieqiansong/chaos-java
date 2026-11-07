package lan.chaos.finance.settlement;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/** 清算轧差净额/全额量化断言测试。 */
class NettingServiceTest {

    private final NettingService service = new NettingService();

    @Test
    void 双向往来只划净额() {
        // A→B 100 + A→B 50 = 150；B→A 80；全额 230，净额 A→B 70
        NettingService.NettingResult result =
                service.net(SettlementSamples.zero(), SettlementSamples.sampleInstructions());

        assertThat(result.getGrossTotal().compareTo(Money.of("230.00", Currency.CNY))).isEqualTo(0);
        assertThat(result.getNetTotal().compareTo(Money.of("70.00", Currency.CNY))).isEqualTo(0);
        assertThat(result.getSettlements()).hasSize(1);
        assertThat(result.getSettlements().get(0).getFrom()).isEqualTo("A");
        assertThat(result.getSettlements().get(0).getTo()).isEqualTo("B");
        assertThat(result.getSettlements().get(0).getAmount()
                .compareTo(Money.of("70.00", Currency.CNY))).isEqualTo(0);
    }

    @Test
    void 完全对冲净额为零() {
        NettingService.NettingResult result = service.net(SettlementSamples.zero(), Arrays.asList(
                new NettingService.Instruction("A", "B", Money.of("100.00", Currency.CNY)),
                new NettingService.Instruction("B", "A", Money.of("100.00", Currency.CNY))));
        assertThat(result.getNetTotal().isZero()).isTrue();
        assertThat(result.getSettlements()).isEmpty();
    }

    @Test
    void 单向全额等于净额() {
        NettingService.NettingResult result = service.net(SettlementSamples.zero(),
                Collections.singletonList(
                        new NettingService.Instruction("A", "B", Money.of("100.00", Currency.CNY))));
        assertThat(result.getNetTotal().compareTo(result.getGrossTotal())).isEqualTo(0);
    }
}
