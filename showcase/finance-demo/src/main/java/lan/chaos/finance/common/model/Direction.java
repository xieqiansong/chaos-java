package lan.chaos.finance.common.model;

/**
 * 借贷方向。复式记账里每一笔资金变动都必须同时记录"从哪来、到哪去"。
 */
public enum Direction {
    /** 借：约定为"增加"账户余额（资产类科目惯例）。 */
    DEBIT,
    /** 贷：约定为"减少"账户余额。 */
    CREDIT
}
