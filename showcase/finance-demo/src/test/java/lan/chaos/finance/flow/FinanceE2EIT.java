package lan.chaos.finance.flow;

import lan.chaos.finance.audit.AuditLogger;
import lan.chaos.finance.batch.CheckpointStore;
import lan.chaos.finance.batch.DayEndJob;
import lan.chaos.finance.batch.WorkItem;
import lan.chaos.finance.common.constant.ClearingStatus;
import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.constant.DiffType;
import lan.chaos.finance.common.constant.OrderStatus;
import lan.chaos.finance.common.model.Account;
import lan.chaos.finance.common.model.Direction;
import lan.chaos.finance.common.model.JournalEntry;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.ReconRecord;
import lan.chaos.finance.common.model.Subject;
import lan.chaos.finance.common.model.Voucher;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.ClearingBillStore;
import lan.chaos.finance.common.store.PaymentOrderStore;
import lan.chaos.finance.common.store.PositionStore;
import lan.chaos.finance.common.store.SubjectStore;
import lan.chaos.finance.common.store.VoucherStore;
import lan.chaos.finance.ledger.DoubleEntryEngine;
import lan.chaos.finance.ledger.LedgerSamples;
import lan.chaos.finance.ledger.ReconcileLedger;
import lan.chaos.finance.payment.PayResult;
import lan.chaos.finance.payment.PaymentSamples;
import lan.chaos.finance.payment.PaymentService;
import lan.chaos.finance.payment.TradeStateMachine;
import lan.chaos.finance.reconciliation.ReconMatcher;
import lan.chaos.finance.settlement.TccClearing;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端集成测试：下单 → 支付(幂等) → 复式记账 → TCC 清算 → 对账 → 日终 → 审计。
 *
 * <p>命名 {@code *IT}：按 AGENTS.md 5.1，由 failsafe 在 {@code mvn verify} 阶段执行，
 * 不挤进 {@code mvn test}。全部内存组件，零外部依赖。
 *
 * <p>本用例的核心断言是「资金守恒」：无论链路怎么走，总账与分户账始终勾稽一致，
 * 且同一笔业务只被记账一次 —— 这就是金融四条底线在代码里的最终体现。
 */
class FinanceE2EIT {

    @Test
    void 全链路下单到日终资金守恒且只记一次账() {
        // ---- 组装基础设施（内存） ----
        AccountStore accounts = new AccountStore();
        SubjectStore subjects = new SubjectStore();
        VoucherStore vouchers = new VoucherStore();
        PaymentOrderStore orders = new PaymentOrderStore();
        PositionStore positions = new PositionStore();

        Subject subject = LedgerSamples.sampleSubject();
        subjects.save(subject);
        Account payer = LedgerSamples.sampleAccount("A", "alice", subject.getCode());
        Account payee = LedgerSamples.sampleAccount("B", "bob", subject.getCode());
        accounts.save(payer);
        accounts.save(payee);
        positions.save(lan.chaos.finance.settlement.SettlementSamples.samplePosition("BANK-A", "1000.00"));
        positions.save(lan.chaos.finance.settlement.SettlementSamples.samplePosition("BANK-B", "1000.00"));

        DoubleEntryEngine engine = new DoubleEntryEngine(accounts, subjects, vouchers);
        PaymentService payment = new PaymentService(orders, new TradeStateMachine());
        TccClearing tcc = new TccClearing(positions, new ClearingBillStore());
        AuditLogger audit = new AuditLogger();

        Money amount = Money.of("100.00", Currency.CNY);
        String bizNo = PaymentSamples.sampleBizNo();

        // ---- 1) 支付：首次 + 重复（幂等） ----
        PayResult first = payment.pay(bizNo, amount);
        PayResult duplicate = payment.pay(bizNo, amount);
        assertThat(first.isDuplicated()).isFalse();
        assertThat(duplicate.isDuplicated()).isTrue();
        assertThat(payment.getChargeCount()).isEqualTo(1);
        assertThat(first.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);

        // ---- 2) 记账：支付成功转为复式凭证（只记一次，与幂等对齐） ----
        Voucher voucher = new Voucher();
        voucher.setId("V-" + bizNo);
        voucher.setBizNo(bizNo);
        voucher.setType("PAYMENT");
        JournalEntry creditPayer = new JournalEntry();
        creditPayer.setId(voucher.getId() + "-E1");
        creditPayer.setAccountId("A");
        creditPayer.setSubjectCode(subject.getCode());
        creditPayer.setDirection(Direction.CREDIT);
        creditPayer.setAmount(amount);
        voucher.addEntry(creditPayer);
        JournalEntry debitPayee = new JournalEntry();
        debitPayee.setId(voucher.getId() + "-E2");
        debitPayee.setAccountId("B");
        debitPayee.setSubjectCode(subject.getCode());
        debitPayee.setDirection(Direction.DEBIT);
        debitPayee.setAmount(amount);
        voucher.addEntry(debitPayee);
        engine.post(voucher);

        // 资金守恒：payer -100 / payee +100 / 总账 0
        assertThat(payer.getBalance().compareTo(Money.of("-100.00", Currency.CNY))).isEqualTo(0);
        assertThat(payee.getBalance().compareTo(amount)).isEqualTo(0);
        new ReconcileLedger(accounts, subjects).assertConsistent();

        // ---- 3) TCC 清算：Try → Confirm，实际划拨 ----
        tcc.tryFreeze("CLR-" + bizNo, "BANK-A", "BANK-B", amount);
        assertThat(tcc.confirm("CLR-" + bizNo).getStatus()).isEqualTo(ClearingStatus.CONFIRMED);
        assertThat(positions.get("BANK-A").getAvailable()
                .compareTo(Money.of("900.00", Currency.CNY))).isEqualTo(0);
        assertThat(positions.get("BANK-B").getAvailable()
                .compareTo(Money.of("1100.00", Currency.CNY))).isEqualTo(0);
        // 全局守恒：单机构在 Confirm 后金额已划出（BANK-A total=900），
        // 但两家机构 total 之和仍是 2000 —— 钱没有凭空多也没有凭空少
        Money globalTotal = positions.get("BANK-A").total().add(positions.get("BANK-B").total());
        assertThat(globalTotal.compareTo(Money.of("2000.00", Currency.CNY))).isEqualTo(0);

        // ---- 4) 对账：本方流水 vs 渠道流水，完全勾对 ----
        Map<String, Money> local = new LinkedHashMap<>();
        local.put(bizNo, amount);
        Map<String, Money> channel = new LinkedHashMap<>();
        channel.put(bizNo, amount);
        List<ReconRecord> reconRecords = new ReconMatcher().match(local, channel);
        assertThat(reconRecords.get(0).getDiffType()).isEqualTo(DiffType.NONE);

        // ---- 5) 日终：总分核对 + 分片批处理 ----
        CheckpointStore checkpoint = new CheckpointStore();
        List<String> handled = new ArrayList<>();
        DayEndJob.DayEndReport report = new DayEndJob().run(
                LocalDate.of(2026, 11, 7), new ReconcileLedger(accounts, subjects),
                DayEndJob.sampleItems(), checkpoint, Money.of("0.00", Currency.CNY),
                (WorkItem w) -> handled.add(w.getId()), null);
        assertThat(report.isLedgerConsistent()).isTrue();
        assertThat(handled).hasSize(5);

        // ---- 6) 审计：留痕且脱敏 ----
        audit.record("payment-engine", "PAY", bizNo, "6222021234567890");
        audit.record("settlement", "CLR", "CLR-" + bizNo, "6222021234567890");
        assertThat(audit.size()).isEqualTo(2);
        assertThat(audit.snapshot().get(0).getMaskedDetail()).doesNotContain("6222021234567890");

        // ---- 最终不变式：全程只记一次账、总分始终一致 ----
        assertThat(vouchers.get("V-" + bizNo).getEntries()).hasSize(2);
        new ReconcileLedger(accounts, subjects).assertConsistent();
    }
}
