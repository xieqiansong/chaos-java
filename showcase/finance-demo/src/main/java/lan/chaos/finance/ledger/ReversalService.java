package lan.chaos.finance.ledger;

import lan.chaos.finance.common.model.Direction;
import lan.chaos.finance.common.model.JournalEntry;
import lan.chaos.finance.common.model.Voucher;
import lan.chaos.finance.common.store.AccountStore;
import lan.chaos.finance.common.store.SubjectStore;
import lan.chaos.finance.common.store.VoucherStore;

/**
 * 冲正服务（对应对话「冲正 Reversal」）。
 *
 * <p>WHY：交易异常（超时/部分成功）时，金融系统<b>绝不删除原单</b>，而是生成一笔
 * 方向相反、金额相同的"红冲"凭证去抵消，原凭证永久保留 —— 这就是可审计性的体现。
 *
 * <p>示例：原凭证 A 贷 100，冲正则生成 A 借 100，两者抵消后净额为 0，但两笔记录都在。
 */
public class ReversalService {

    private final DoubleEntryEngine engine;
    private final VoucherStore voucherStore;

    public ReversalService(DoubleEntryEngine engine, VoucherStore voucherStore) {
        this.engine = engine;
        this.voucherStore = voucherStore;
    }

    public Voucher reverse(String originalVoucherId) {
        Voucher original = voucherStore.get(originalVoucherId);

        Voucher rev = new Voucher();
        rev.setId(original.getId() + "-REV");
        rev.setBizNo(original.getBizNo() + "-REV");
        rev.setType(original.getType() + "_REV");

        for (JournalEntry entry : original.getEntries()) {
            JournalEntry reversed = new JournalEntry();
            reversed.setId(entry.getId() + "-REV");
            reversed.setAccountId(entry.getAccountId());
            reversed.setSubjectCode(entry.getSubjectCode());
            // 方向翻转：借变贷、贷变借
            reversed.setDirection(entry.getDirection() == Direction.DEBIT ? Direction.CREDIT : Direction.DEBIT);
            reversed.setAmount(entry.getAmount());
            rev.addEntry(reversed);
        }

        Voucher posted = engine.post(rev);
        original.setReversed(true); // 原单仅标记，金额不动、不删除
        return posted;
    }
}
