package lan.chaos.finance.common.model;

import lan.chaos.finance.common.constant.ClearingStatus;
import lombok.Data;

/** 清算单：一笔待结算的资金划拨指令，带 TCC 状态。 */
@Data
public class ClearingBill {
    private String bizNo;
    private String fromInstitution;
    private String toInstitution;
    private Money amount;
    private ClearingStatus status;
}
