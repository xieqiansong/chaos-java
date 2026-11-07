package lan.chaos.finance.ledger;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.exception.UnbalancedVoucherException;
import lan.chaos.finance.common.model.Account;
import lan.chaos.finance.common.model.Direction;
import lan.chaos.finance.common.model.JournalEntry;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.Subject;
import lan.chaos.finance.common.model.Voucher;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.SubjectStore;
import lan.chaos.finance.common.store.VoucherStore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 复式记账引擎（P1 核心）。
 *
 * <p>WHY（对应对话「复式记账 / 数据准确性 / 可审计性」）：
 * <ul>
 *   <li><b>借贷必平衡</b>：一笔凭证的借总计必须等于贷总计，否则直接拒绝（资损的第一道防线）。</li>
 *   <li><b>账户分层同步</b>：每笔分录同时更新分户账余额与对应总账科目余额，保证总分勾稽可成立。</li>
 *   <li><b>append-only</b>：凭证与分录只追加，绝不修改/删除原记录，满足可审计性。</li>
 * </ul>
 *
 * <p>防御性思维：本方法要么整体成功、要么抛异常（无"部分记账"的中间状态，见思维②）。
 *
 * <p>防御性思维①（并发，对应对话 Java 映射表「同一账户不被并发重复扣减」）：
 * 一笔凭证会同时改多个分户账 + 总账，{@code getBalance().add(delta)} 是
 * read-modify-write，并发下会<b>丢失更新</b>（两个线程读到同一旧值，各加一次只生效一次）。
 * 因此整笔记账必须在同一个临界区内完成 —— 等价于数据库的「事务 + 行锁」。
 * 注意不能只对单个账户做 CAS：一笔凭证涉及多账户，逐账户 CAS 会产生"部分成功"，
 * 破坏复式记账的原子性。
 *
 * <p>生产映射：{@code version} 字段在这里记录变更次数；真实库用乐观锁
 * {@code UPDATE account SET balance=?, version=version+1 WHERE id=? AND version=?}，
 * 影响行数为 0 时重试或抛并发冲突；热点账户则改用行锁串行化。
 */
public class DoubleEntryEngine {

    private final AccountStore accountStore;
    private final SubjectStore subjectStore;
    private final VoucherStore voucherStore;

    public DoubleEntryEngine(AccountStore accountStore, SubjectStore subjectStore, VoucherStore voucherStore) {
        this.accountStore = accountStore;
        this.subjectStore = subjectStore;
        this.voucherStore = voucherStore;
    }

    public Voucher post(Voucher voucher) {
        validateBalanced(voucher);

        // 以 accountStore 为全局锁：整笔凭证的"读余额 → 改多账户 → 改总账 → 落凭证"
        // 原子完成，杜绝并发丢失更新与部分记账。真实库对应一个数据库事务。
        synchronized (accountStore) {
            for (JournalEntry entry : voucher.getEntries()) {
                Account account = accountStore.get(entry.getAccountId());
                Subject subject = subjectStore.get(entry.getSubjectCode());

                // 约定：DEBIT 增加余额，CREDIT 减少余额（两账同步，保持勾稽）
                Money delta = entry.getDirection() == Direction.DEBIT
                        ? entry.getAmount()
                        : entry.getAmount().negate();

                account.setBalance(account.getBalance().add(delta));
                account.setVersion(account.getVersion() + 1);
                subject.setBalance(subject.getBalance().add(delta));
            }

            voucher.setCreatedAt(LocalDateTime.now());
            return voucherStore.save(voucher);
        }
    }

    /** 校验同一币种借贷必相等；跨币种或多币种不平衡均拒绝。 */
    private void validateBalanced(Voucher voucher) {
        Map<Currency, BigDecimal> debit = new HashMap<>();
        Map<Currency, BigDecimal> credit = new HashMap<>();
        for (JournalEntry entry : voucher.getEntries()) {
            Currency c = entry.getAmount().getCurrency();
            BigDecimal amt = entry.getAmount().getAmount();
            if (entry.getDirection() == Direction.DEBIT) {
                debit.merge(c, amt, BigDecimal::add);
            } else {
                credit.merge(c, amt, BigDecimal::add);
            }
        }
        if (!debit.keySet().equals(credit.keySet())) {
            throw new UnbalancedVoucherException("借贷币种不一致: 借=" + debit.keySet() + " 贷=" + credit.keySet());
        }
        for (Currency c : debit.keySet()) {
            if (debit.get(c).compareTo(credit.getOrDefault(c, BigDecimal.ZERO)) != 0) {
                throw new UnbalancedVoucherException(
                        "借贷不平衡(" + c + "): 借=" + debit.get(c) + " 贷=" + credit.get(c));
            }
        }
    }
}
