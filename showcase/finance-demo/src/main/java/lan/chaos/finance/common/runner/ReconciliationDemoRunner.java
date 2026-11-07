package lan.chaos.finance.common.runner;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.reconciliation.DiffHandler;
import lan.chaos.finance.reconciliation.ReconMatcher;
import lan.chaos.finance.reconciliation.ReconParser;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 控制台演示：对账勾对与差错分类（P3）。正确性由 {@code *Test} 断言。 */
@Component
@org.springframework.core.annotation.Order(5)
public class ReconciliationDemoRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("===== [reconciliation] 对账演示（P3） =====");
        Map<String, Money> channel = new ReconParser().parseCsv(ReconParser.sampleChannelFile());

        Map<String, Money> local = new LinkedHashMap<>();
        local.put("P001", Money.of("100.00", Currency.CNY)); // NONE
        local.put("P002", Money.of("199.00", Currency.CNY)); // MISMATCH
        local.put("P003", Money.of("30.00", Currency.CNY));  // LONG
        // P004 本方缺失 → SHORT

        ReconMatcher matcher = new ReconMatcher();
        List<lan.chaos.finance.common.model.ReconRecord> records = matcher.match(local, channel);
        System.out.println("勾对结果: " + matcher.countByType(records));
        DiffHandler handler = new DiffHandler();
        handler.handle(records);
        handler.getActions().forEach(a -> System.out.println("  差错动作: " + a));
        System.out.println("===== [reconciliation] 演示结束 =====");
    }
}
