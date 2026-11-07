package lan.chaos.finance.common.model;

import lombok.Data;

/**
 * 总账科目（机构层面的核算单元）。
 *
 * <p>WHY：所有客户的钱按科目汇总到总账。{@link Account} 的余额之和必须等于本科目余额，
 * 否则就是账务悬案。{@link lan.chaos.finance.ledger.ReconcileLedger} 负责校验这条勾稽。
 */
@Data
public class Subject {
    private String code;
    private String name;
    private Money balance;
}
