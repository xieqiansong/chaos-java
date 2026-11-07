package lan.chaos.finance.common.model;

import lombok.Data;

/**
 * 机构头寸（清算账户）：可用余额 + 冻结金额。
 *
 * <p>WHY：TCC 的 Try 阶段不真扣钱，而是把资金从「可用」挪到「冻结」，
 * 这样中途失败时只需解冻（Cancel），不会造成资金丢失或透支。
 */
@Data
public class Position {
    private String institution;
    private Money available;
    private Money frozen;

    /** 总头寸 = 可用 + 冻结（守恒，用于断言 TCC 全程资金不丢不多）。 */
    public Money total() {
        return available.add(frozen);
    }
}
