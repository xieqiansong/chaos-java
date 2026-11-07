package lan.chaos.finance.money;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Money 金额值对象与运算的断言测试（AGENTS.md 第四章：单元测试为及格线）。
 * 纯 JDK，零外部依赖，{@code mvn test} 直接跑。
 */
class MoneyArithmeticTest {

    @Test
    void doubleTrap_证明BigDecimal无浮点误差() {
        Money x = Money.of("0.1", Currency.CNY);
        Money y = Money.of("0.2", Currency.CNY);
        assertThat(x.add(y).toString()).isEqualTo("CNY 0.30");
        assertThat(x.add(y).compareTo(Money.of("0.3", Currency.CNY))).isEqualTo(0);
    }

    @Test
    void ofMinor_与of字符串等价() {
        assertThat(Money.ofMinor(10000, Currency.CNY)).isEqualTo(Money.of("100.00", Currency.CNY));
    }

    @Test
    void 加减不变式() {
        Money balance = Money.of("100.00", Currency.CNY);
        Money debit = Money.of("30.50", Currency.CNY);
        assertThat(balance.subtract(debit).compareTo(Money.of("69.50", Currency.CNY))).isEqualTo(0);
        // 扣款再原路加回，余额应回到原值
        assertThat(balance.subtract(debit).add(debit).compareTo(balance)).isEqualTo(0);
    }

    @Test
    void 除法舍入() {
        Money ten = Money.of("10.00", Currency.CNY);
        assertThat(ten.divide(3, RoundingMode.HALF_UP).toString()).isEqualTo("CNY 3.33");
        assertThat(ten.divide(3, RoundingMode.DOWN).toString()).isEqualTo("CNY 3.33");
    }

    @Test
    void 不同币种禁止直接运算() {
        Money cny = Money.of("1.00", Currency.CNY);
        Money usd = Money.of("1.00", Currency.USD);
        assertThatThrownBy(() -> cny.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("币种不一致");
    }

    @Test
    void 样例工厂产出预期值() {
        assertThat(MoneyArithmetic.sampleMoney().compareTo(Money.of("100.00", Currency.CNY))).isEqualTo(0);
        assertThat(MoneyArithmetic.sampleMinor().compareTo(Money.of("100.00", Currency.CNY))).isEqualTo(0);
    }
}
