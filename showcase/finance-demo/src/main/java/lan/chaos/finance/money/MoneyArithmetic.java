package lan.chaos.finance.money;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;

import java.math.RoundingMode;

/**
 * 金额运算能力包（P0 地基）。
 *
 * <p>每个方法演示一个知识点，并打印「输入 → 输出」，满足 AGENTS.md 第三章"可观察输出"要求。
 * 真正的断言在 {@code MoneyArithmeticTest} 中，本类偏控制台把玩。
 */
public final class MoneyArithmetic {

    private MoneyArithmetic() {
    }

    /**
     * WHY：double 浮点误差是资金系统的隐形杀手。对照展示 double 与 BigDecimal 的差别。
     */
    public static void doubleTrap() {
        double a = 0.1, b = 0.2;
        System.out.println("[double] 0.1 + 0.2 = " + (a + b) + "  ← 误差！");

        Money x = Money.of("0.1", Currency.CNY);
        Money y = Money.of("0.2", Currency.CNY);
        System.out.println("[Money] 0.1 + 0.2 = " + x.add(y) + "  ← 精确");
    }

    public static void construct() {
        Money fromStr = Money.of("100.00", Currency.CNY);
        Money fromMinor = Money.ofMinor(10000, Currency.CNY); // 以"分"构造
        System.out.println("of(\"100.00\")      = " + fromStr);
        System.out.println("ofMinor(10000) = " + fromMinor + "  （两种构造等价）");
    }

    public static void addSubtract() {
        Money balance = Money.of("100.00", Currency.CNY);
        Money debit = Money.of("30.50", Currency.CNY);
        System.out.println("余额 " + balance + " 扣款 " + debit + " = " + balance.subtract(debit));
        System.out.println("再原路加回 " + debit + " = " + balance.subtract(debit).add(debit) + "  （不变式成立）");
    }

    /**
     * WHY：金融里除法（费率/分摊）常"除不尽"，必须明确舍入模式，否则每张账单差几厘累积成资损。
     */
    public static void rounding() {
        Money ten = Money.of("10.00", Currency.CNY);
        System.out.println("10 / 3  HALF_UP = " + ten.divide(3, RoundingMode.HALF_UP) + "  （四舍五入）");
        System.out.println("10 / 3  DOWN    = " + ten.divide(3, RoundingMode.DOWN) + "  （截断）");
    }

    public static void compare() {
        Money a = Money.of("100.00", Currency.CNY);
        Money b = Money.of("99.99", Currency.CNY);
        System.out.println(a + " > " + b + " ? " + a.isGreaterThan(b));
        System.out.println("币种不一致运算：CNY + USD 会抛 IllegalArgumentException（资金安全第一关）");
    }

    /** 样例数据工厂：测试与演示共用，调用方无需自备输入（AGENTS.md 第三章）。 */
    public static Money sampleMoney() {
        return Money.of("100.00", Currency.CNY);
    }

    public static Money sampleMinor() {
        return Money.ofMinor(10000, Currency.CNY);
    }
}
