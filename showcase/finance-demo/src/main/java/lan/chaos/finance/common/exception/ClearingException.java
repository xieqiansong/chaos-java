package lan.chaos.finance.common.exception;

/** 清算状态非法（如重复 Confirm、Cancel 后 Confirm、余额不足冻结等）。 */
public class ClearingException extends RuntimeException {
    public ClearingException(String message) {
        super(message);
    }
}
