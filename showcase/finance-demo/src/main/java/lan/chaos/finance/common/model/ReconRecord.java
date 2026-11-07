package lan.chaos.finance.common.model;

import lan.chaos.finance.common.constant.DiffType;
import lombok.Data;

/** 对账记录：本方流水与渠道流水按 bizNo 勾对的结果。 */
@Data
public class ReconRecord {
    private String bizNo;
    /** 本方金额（无此笔时为 null） */
    private Money localAmount;
    /** 渠道金额（无此笔时为 null） */
    private Money channelAmount;
    private DiffType diffType;
}
