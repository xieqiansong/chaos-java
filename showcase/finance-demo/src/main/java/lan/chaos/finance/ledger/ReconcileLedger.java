package lan.chaos.finance.ledger;

import lan.chaos.finance.common.exception.ReconcileException;
import lan.chaos.finance.common.model.Account;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Subject;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.SubjectStore;

/**
 * 总分勾稽（对应对话「账户分层：分户账 → 总账」）。
 *
 * <p>WHY：每个客户的余额（分户账）加起来，必须等于机构对应科目的余额（总账）。
 * 若有人绕过记账引擎直接改了某个分户账，这里会立刻发现不一致 —— 这是"数据准确性"的兜底校验。
 */
public class ReconcileLedger {

    private final AccountStore accountStore;
    private final SubjectStore subjectStore;

    public ReconcileLedger(AccountStore accountStore, SubjectStore subjectStore) {
        this.accountStore = accountStore;
        this.subjectStore = subjectStore;
    }

    /**
     * 校验所有科目的「分户账汇总 == 总账科目余额」，不一致抛 {@link ReconcileException}。
     *
     * <p>WHY 加锁：与 {@code DoubleEntryEngine.post} 共用 accountStore 监视器，
     * 保证读数时不会看到"分户账已改、总账未改"的撕裂中间态（日终核对必须是一致性快照）。
     */
    public void assertConsistent() {
        synchronized (accountStore) {
            for (Subject subject : subjectStore.all()) {
                Money sum = Money.of("0.00", subject.getBalance().getCurrency());
                for (Account account : accountStore.all()) {
                    if (account.getSubjectCode().equals(subject.getCode())) {
                        sum = sum.add(account.getBalance());
                    }
                }
                if (sum.compareTo(subject.getBalance()) != 0) {
                    throw new ReconcileException(
                            "总分不符: 科目 " + subject.getCode()
                                    + " 分户汇总=" + sum + " 总账=" + subject.getBalance());
                }
            }
        }
    }
}
