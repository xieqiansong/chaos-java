package lan.chaos.finance.common.exception;

/**
 * 凭证借贷不平衡异常。复式记账的硬性约束：同一币种下借总计必须等于贷总计。
 */
public class UnbalancedVoucherException extends RuntimeException {
    public UnbalancedVoucherException(String message) {
        super(message);
    }
}
