package lan.chaos.finance.settlement;

import lan.chaos.finance.common.model.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 清算轧差（P3，对应对话「清算：算清谁欠谁 / 净额·全额清算」）。
 *
 * <p>WHY：跨行跨机构之间 A 欠 B 一堆笔、B 也欠 A 一堆笔，若每笔都实际划钱（全额 gross）
 * 成本高、流动性占用大。轧差（netting）把多笔应收应付互相抵消，只划净额。
 * 例：A 应付 B 100 万、B 应付 A 80 万 → 净额只需 A 划 20 万给 B。
 *
 * <p>一条指令 = 「from 应付 to 一笔钱」。按 (from,to) 聚合后净额结算，
 * 输出「全额总额 vs 净额总额」的量化对比（AGENTS.md 优化类 demo 的量化结论风格）。
 */
public class NettingService {

    /** 一条清算指令：from 应付 to 指定金额。 */
    public static class Instruction {
        private final String from;
        private final String to;
        private final Money amount;

        public Instruction(String from, String to, Money amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }
    }

    /** 轧差结果。 */
    public static class NettingResult {
        private final Money grossTotal;
        private final Money netTotal;
        private final List<Settlement> settlements;

        NettingResult(Money grossTotal, Money netTotal, List<Settlement> settlements) {
            this.grossTotal = grossTotal;
            this.netTotal = netTotal;
            this.settlements = settlements;
        }

        public Money getGrossTotal() {
            return grossTotal;
        }

        public Money getNetTotal() {
            return netTotal;
        }

        public List<Settlement> getSettlements() {
            return settlements;
        }
    }

    /** 最终一笔实际划拨。 */
    public static class Settlement {
        private final String from;
        private final String to;
        private final Money amount;

        Settlement(String from, String to, Money amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public Money getAmount() {
            return amount;
        }

        @Override
        public String toString() {
            return from + " → " + to + " : " + amount;
        }
    }

    public NettingResult net(Money zero, List<Instruction> instructions) {
        Money gross = zero;
        // key = "from>to"，value = 该方向累计净额（用 BigDecimal 累加，最后转回 Money）
        Map<String, BigDecimal> pairNet = new LinkedHashMap<>();
        Map<String, String[]> pairNames = new LinkedHashMap<>();

        for (Instruction ins : instructions) {
            gross = gross.add(ins.amount);
            String key = ins.from + ">" + ins.to;
            pairNet.merge(key, ins.amount.getAmount(), BigDecimal::add);
            pairNames.putIfAbsent(key, new String[]{ins.from, ins.to});
        }

        // 双向对冲：对每对机构，正反两个方向的金额相抵，只留净额方向
        List<Settlement> settlements = new ArrayList<>();
        Money netTotal = zero;
        for (Map.Entry<String, BigDecimal> e : pairNet.entrySet()) {
            String[] names = pairNames.get(e.getKey());
            String reverseKey = names[1] + ">" + names[0];
            BigDecimal reverse = pairNet.getOrDefault(reverseKey, BigDecimal.ZERO);
            BigDecimal diff = e.getValue().subtract(reverse);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                Money amt = Money.of(diff.toPlainString(), zero.getCurrency());
                settlements.add(new Settlement(names[0], names[1], amt));
                netTotal = netTotal.add(amt);
            }
        }
        return new NettingResult(gross, netTotal, settlements);
    }
}
