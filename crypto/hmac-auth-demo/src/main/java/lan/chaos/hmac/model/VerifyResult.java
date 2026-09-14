package lan.chaos.hmac.model;

/** 上报请求校验结果。 */
public final class VerifyResult {

    private final boolean passed;
    private final String reason;
    private final long costNanos;

    private VerifyResult(boolean passed, String reason, long costNanos) {
        this.passed = passed;
        this.reason = reason;
        this.costNanos = costNanos;
    }

    public static VerifyResult ok(long costNanos) {
        return new VerifyResult(true, "OK", costNanos);
    }

    public static VerifyResult fail(String reason) {
        return new VerifyResult(false, reason, 0L);
    }

    public boolean isPassed() {
        return passed;
    }

    public String getReason() {
        return reason;
    }

    public long getCostNanos() {
        return costNanos;
    }

    @Override
    public String toString() {
        return passed ? "PASS" : "REJECT: " + reason;
    }
}
