package lan.chaos.finance.common.model;

import lan.chaos.finance.common.constant.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 不可变金额值对象 —— 金融资金运算的基石。
 *
 * <p>WHY：计算机用二进制表示小数，{@code double} 会有不可逆的浮点误差
 * （{@code 0.1 + 0.2 = 0.30000000000000004}）。在资金场景，差一分钱就是资损事故。
 * 因此金额必须以 {@link BigDecimal} 承载，且构造入口<b>只接受 String / 最小货币单位(long)</b>，
 * 绝不直接接受 {@code double}（见 {@code MoneyArithmetic.doubleTrap()} 的对照演示）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>不可变：所有运算返回新的 {@code Money}，原对象不变（线程安全、无副作用）。</li>
 *   <li>币种约束：不同币种禁止直接加减，强制 {@link #requireSameCurrency} 前置校验。</li>
 *   <li>精度固定：构造即按币种默认小数位 {@link RoundingMode#HALF_UP} 取整，杜绝后续漂移。</li>
 * </ul>
 */
public final class Money {

    private final BigDecimal amount;
    private final Currency currency;
    private final int scale;
    private final RoundingMode roundingMode;

    private Money(BigDecimal amount, Currency currency, int scale, RoundingMode roundingMode) {
        this.amount = amount.setScale(scale, roundingMode);
        this.currency = currency;
        this.scale = scale;
        this.roundingMode = roundingMode;
    }

    /**
     * 以字符串构造金额（推荐，零浮点误差）。
     */
    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency, currency.getDefaultScale(), RoundingMode.HALF_UP);
    }

    /**
     * 以最小货币单位（如"分"）构造，最安全：整数运算，彻底绕开小数。
     * 例：{@code ofMinor(10000, CNY)} == 100.00 元。
     */
    public static Money ofMinor(long minorUnits, Currency currency) {
        BigDecimal amt = BigDecimal.valueOf(minorUnits).movePointLeft(currency.getDefaultScale());
        return new Money(amt, currency, currency.getDefaultScale(), RoundingMode.HALF_UP);
    }

    /** 防御性思维①：被并发读取安全（不可变）；加减本身不修改原对象。 */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), currency, scale, roundingMode);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), currency, scale, roundingMode);
    }

    public Money multiply(long factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), currency, scale, roundingMode);
    }

    /** 除法必须显式指定舍入模式 —— 金融里"除不尽"必须明确怎么舍（四舍五入/截断）。 */
    public Money divide(long divisor, RoundingMode mode) {
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), scale, mode), currency, scale, mode);
    }

    /** 取反，用于冲正（生成方向相反的金额）。 */
    public Money negate() {
        return new Money(this.amount.negate(), currency, scale, roundingMode);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("币种不一致，禁止直接运算：" + currency + " vs " + other.currency);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public int getScale() {
        return scale;
    }

    /** 展示形式：{@code CNY 100.00}。 */
    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money)) {
            return false;
        }
        Money money = (Money) o;
        return currency == money.currency && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount);
    }
}
