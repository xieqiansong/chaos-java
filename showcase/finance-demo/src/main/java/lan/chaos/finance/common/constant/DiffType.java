package lan.chaos.finance.common.constant;

/**
 * 对账差异分类。
 *
 * <p>WHY：系统间（支付引擎 vs 渠道）定时比对流水，发现差异后必须<b>分类</b>才能进入
 * 对应差错处理流程（对话「对账 / 差错处理」）。
 */
public enum DiffType {
    /** 完全勾对 */
    NONE,
    /** 长款：本方多记（渠道无此笔 / 本方金额更大） */
    LONG,
    /** 短款：本方少记（渠道有此笔 / 本方金额更小） */
    SHORT,
    /** 金额不符：双方都有该笔但金额不一致 */
    MISMATCH
}
