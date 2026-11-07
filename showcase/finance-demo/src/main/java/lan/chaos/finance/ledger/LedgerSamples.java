package lan.chaos.finance.ledger;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Account;
import lan.chaos.finance.common.model.Direction;
import lan.chaos.finance.common.model.JournalEntry;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Subject;
import lan.chaos.finance.common.model.Voucher;

/**
 * 样例数据工厂（AGENTS.md 第三章：调用方无需自备输入）。
 * 构造一个最小可演示的"转账"场景：A 转 100 给 B，两者同属一个科目。
 */
public final class LedgerSamples {

    private LedgerSamples() {
    }

    public static Subject sampleSubject() {
        Subject subject = new Subject();
        subject.setCode("1001");
        subject.setName("客户存款");
        subject.setBalance(Money.of("0.00", Currency.CNY));
        return subject;
    }

    public static Account sampleAccount(String id, String ownerId, String subjectCode) {
        Account account = new Account();
        account.setId(id);
        account.setOwnerId(ownerId);
        account.setSubjectCode(subjectCode);
        account.setBalance(Money.of("0.00", Currency.CNY));
        account.setVersion(0);
        return account;
    }

    /** 构造一笔 A→B 转账凭证：A 贷(CREDIT) 减少、B 借(DEBIT) 增加，借贷平衡。 */
    public static Voucher sampleTransferVoucher(String fromAccId, String toAccId, String subjectCode, Money amount) {
        Voucher voucher = new Voucher();
        voucher.setId("V-" + fromAccId + "-" + toAccId);
        voucher.setBizNo("BIZ-TRANSFER-001");
        voucher.setType("TRANSFER");

        JournalEntry creditFrom = new JournalEntry();
        creditFrom.setId(voucher.getId() + "-E1");
        creditFrom.setAccountId(fromAccId);
        creditFrom.setSubjectCode(subjectCode);
        creditFrom.setDirection(Direction.CREDIT);
        creditFrom.setAmount(amount);
        voucher.addEntry(creditFrom);

        JournalEntry debitTo = new JournalEntry();
        debitTo.setId(voucher.getId() + "-E2");
        debitTo.setAccountId(toAccId);
        debitTo.setSubjectCode(subjectCode);
        debitTo.setDirection(Direction.DEBIT);
        debitTo.setAmount(amount);
        voucher.addEntry(debitTo);

        return voucher;
    }
}
