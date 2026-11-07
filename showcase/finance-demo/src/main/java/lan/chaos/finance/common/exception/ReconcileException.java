package lan.chaos.finance.common.exception;

/**
 * 总分勾稽不一致异常。分户账余额汇总与总账科目余额不符，属于账务事故。
 */
public class ReconcileException extends RuntimeException {
    public ReconcileException(String message) {
        super(message);
    }
}
