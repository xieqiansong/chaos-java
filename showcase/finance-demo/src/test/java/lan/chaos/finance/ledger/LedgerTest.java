package lan.chaos.finance.ledger;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.exception.ReconcileException;
import lan.chaos.finance.common.exception.UnbalancedVoucherException;
import lan.chaos.finance.common.model.Account;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Subject;
import lan.chaos.finance.common.model.Voucher;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.SubjectStore;
import lan.chaos.finance.common.store.VoucherStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 复式记账引擎 / 冲正 / 总分勾稽 断言测试（纯内存，零外部依赖）。
 */
class LedgerTest {

    private AccountStore accountStore;
    private SubjectStore subjectStore;
    private VoucherStore voucherStore;
    private DoubleEntryEngine engine;
    private ReconcileLedger reconcile;

    @BeforeEach
    void setUp() {
        accountStore = new AccountStore();
        subjectStore = new SubjectStore();
        voucherStore = new VoucherStore();
        Subject subject = LedgerSamples.sampleSubject();
        subjectStore.save(subject);
        accountStore.save(LedgerSamples.sampleAccount("A", "alice", subject.getCode()));
        accountStore.save(LedgerSamples.sampleAccount("B", "bob", subject.getCode()));
        engine = new DoubleEntryEngine(accountStore, subjectStore, voucherStore);
        reconcile = new ReconcileLedger(accountStore, subjectStore);
    }

    @Test
    void 借贷不平衡的凭证被拒绝() {
        Voucher bad = LedgerSamples.sampleTransferVoucher("A", "B", "1001", Money.of("100.00", Currency.CNY));
        // 篡改：让贷方变成 90，制造不平衡
        bad.getEntries().get(0).setAmount(Money.of("90.00", Currency.CNY));
        assertThatThrownBy(() -> engine.post(bad))
                .isInstanceOf(UnbalancedVoucherException.class);
    }

    @Test
    void 记账后余额正确且总分勾稽通过() {
        Voucher transfer = LedgerSamples.sampleTransferVoucher("A", "B", "1001", Money.of("100.00", Currency.CNY));
        engine.post(transfer);

        Account a = accountStore.get("A");
        Account b = accountStore.get("B");
        assertThat(a.getBalance().compareTo(Money.of("-100.00", Currency.CNY))).isEqualTo(0);
        assertThat(b.getBalance().compareTo(Money.of("100.00", Currency.CNY))).isEqualTo(0);
        // 总分勾稽：两分户汇总(0) == 总账(0)
        assertThat(reconcile).isNotNull();
        // 不抛异常即通过
        new ReconcileLedger(accountStore, subjectStore).assertConsistent();
    }

    @Test
    void 冲正后净额归零且原单保留() {
        Voucher transfer = LedgerSamples.sampleTransferVoucher("A", "B", "1001", Money.of("100.00", Currency.CNY));
        engine.post(transfer);
        new ReversalService(engine, voucherStore).reverse(transfer.getId());

        Account a = accountStore.get("A");
        Account b = accountStore.get("B");
        assertThat(a.getBalance().isZero()).isTrue();
        assertThat(b.getBalance().isZero()).isTrue();
        // 原单未被删除，仅标记 reversed
        assertThat(voucherStore.get(transfer.getId()).isReversed()).isTrue();
    }

    @Test
    void 分户账被篡改时总分勾稽报错() {
        engine.post(LedgerSamples.sampleTransferVoucher("A", "B", "1001", Money.of("100.00", Currency.CNY)));
        // 绕过记账引擎，直接改分户账余额，破坏勾稽
        Account a = accountStore.get("A");
        a.setBalance(a.getBalance().add(Money.of("1.00", Currency.CNY)));
        assertThatThrownBy(() -> reconcile.assertConsistent())
                .isInstanceOf(ReconcileException.class);
    }
}
