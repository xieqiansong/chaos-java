package lan.chaos.finance.common.runner;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Account;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Subject;
import lan.chaos.finance.common.model.Voucher;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.SubjectStore;
import lan.chaos.finance.common.store.VoucherStore;
import lan.chaos.finance.ledger.DoubleEntryEngine;
import lan.chaos.finance.ledger.LedgerSamples;
import lan.chaos.finance.ledger.ReconcileLedger;
import lan.chaos.finance.ledger.ReversalService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 控制台演示：复式记账 → 冲正 → 总分勾稽（P1）。仅用于把玩；正确性由 {@code *Test} 断言。
 */
@Component
@org.springframework.core.annotation.Order(2)
public class LedgerDemoRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        AccountStore accountStore = new AccountStore();
        SubjectStore subjectStore = new SubjectStore();
        VoucherStore voucherStore = new VoucherStore();

        Subject subject = LedgerSamples.sampleSubject();
        subjectStore.save(subject);
        Account a = LedgerSamples.sampleAccount("A", "alice", subject.getCode());
        Account b = LedgerSamples.sampleAccount("B", "bob", subject.getCode());
        accountStore.save(a);
        accountStore.save(b);

        DoubleEntryEngine engine = new DoubleEntryEngine(accountStore, subjectStore, voucherStore);
        Voucher transfer = LedgerSamples.sampleTransferVoucher("A", "B", subject.getCode(), Money.of("100.00", Currency.CNY));
        engine.post(transfer);

        System.out.println("===== [ledger] 复式记账演示（P1） =====");
        System.out.println("转账后 A=" + a.getBalance() + " B=" + b.getBalance() + " 总账=" + subject.getBalance());
        new ReconcileLedger(accountStore, subjectStore).assertConsistent();
        System.out.println("总分勾稽：通过");

        ReversalService reversal = new ReversalService(engine, voucherStore);
        reversal.reverse(transfer.getId());
        System.out.println("冲正后 A=" + a.getBalance() + " B=" + b.getBalance() + " 总账=" + subject.getBalance() + "（净额归零，原单保留）");
        System.out.println("===== [ledger] 演示结束 =====");
    }
}
