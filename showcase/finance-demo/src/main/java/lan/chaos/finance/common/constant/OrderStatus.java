package lan.chaos.finance.common.constant;

/**
 * 交易订单状态（生命周期）。
 *
 * <p>WHY：金融交易的生命周期必须由明确的状态迁移规则驱动，禁止"待支付直接跳退款"
 * 这类非法跃迁。合法迁移规则集中在 {@link lan.chaos.finance.payment.TradeStateMachine}，
 * 而非散落在业务代码的 if-else 里（对话：状态机比拼状态可靠得多）。
 */
public enum OrderStatus {
    /** 待支付 */
    INIT,
    /** 支付中 */
    PAYING,
    /** 支付成功 */
    PAID,
    /** 支付失败 */
    FAILED,
    /** 已退款 */
    REFUNDED
}
