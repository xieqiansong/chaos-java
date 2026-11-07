package lan.chaos.finance.common.constant;

/**
 * 清算单状态（TCC 三阶段）。
 *
 * <p>WHY：跨服务（交易→账务→清算）的强一致事务往往不可行或代价过高，
 * 因此拆成 Try（冻结，先占住钱不真扣）/ Confirm（真扣）/ Cancel（解冻冲正），
 * 用「最终一致性」替代一次性强事务（对话「最终一致性」）。
 */
public enum ClearingStatus {
    /** 一阶段：头寸已冻结，资金未实际划拨 */
    TRY,
    /** 二阶段提交：实际划拨完成 */
    CONFIRMED,
    /** 二阶段回滚：解冻，资金归还 */
    CANCELED
}
