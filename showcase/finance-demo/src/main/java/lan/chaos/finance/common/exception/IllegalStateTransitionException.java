package lan.chaos.finance.common.exception;

/**
 * 非法状态跃迁异常。交易订单不允许从当前状态跳到目标状态（如 INIT 直接 REFUNDED）。
 */
public class IllegalStateTransitionException extends RuntimeException {
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
