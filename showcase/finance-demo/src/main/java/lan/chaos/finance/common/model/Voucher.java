package lan.chaos.finance.common.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 凭证（Voucher）—— 一笔交易的借贷分录集合，是记账的入口。
 *
 * <p>WHY：互联网习惯"直接改余额字段"；金融必须用凭证承载变动，因为凭证可审计、可冲正。
 * 异常时绝不删除原凭证，而是生成一笔反向凭证（见 {@link lan.chaos.finance.ledger.ReversalService}）。
 */
@Data
public class Voucher {
    private String id;
    /** 业务流水号（幂等键，见 payment 包） */
    private String bizNo;
    private String type;
    private List<JournalEntry> entries = new ArrayList<>();
    private LocalDateTime createdAt;
    /** 是否已冲正（原单保留，仅标记） */
    private boolean reversed;

    public void addEntry(JournalEntry entry) {
        entry.setVoucherId(this.id);
        entries.add(entry);
    }
}
