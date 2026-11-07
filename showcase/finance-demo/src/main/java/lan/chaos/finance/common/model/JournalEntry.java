package lan.chaos.finance.common.model;

import lombok.Data;

/**
 * 分录（Journal Entry）—— 复式记账的最小记录单元，append-only（只追加，不可改不可删）。
 *
 * <p>WHY：金融账务的可审计性就来自"每一笔变动都留下不可篡改的痕迹"。
 * 一笔 {@link Voucher} 至少包含一借一贷两条分录，且借贷金额必相等。
 */
@Data
public class JournalEntry {
    private String id;
    private String voucherId;
    private String accountId;
    private String subjectCode;
    private Direction direction;
    private Money amount;
}
