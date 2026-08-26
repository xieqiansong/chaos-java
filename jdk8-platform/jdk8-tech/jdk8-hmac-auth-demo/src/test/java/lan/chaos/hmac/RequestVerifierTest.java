package lan.chaos.hmac;

import lan.chaos.hmac.core.HmacSigner;
import lan.chaos.hmac.core.SecretKeyStore;
import lan.chaos.hmac.model.ReportRequest;
import lan.chaos.hmac.model.VerifyResult;
import lan.chaos.hmac.verify.ReplayGuard;
import lan.chaos.hmac.verify.RequestVerifier;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 完整校验链：时间窗 / 防重放 / 幂等去重。 */
public class RequestVerifierTest {

    private static final String SECRET = "test-secret";
    private static final String PATH = "/v1/report";

    private long now;
    private RequestVerifier verifier;

    @Before
    public void setUp() {
        this.now = System.currentTimeMillis() / 1000L;
        this.verifier = new RequestVerifier(
                new SecretKeyStore(SECRET),
                new ReplayGuard(5, 60_000L),
                300);
    }

    @Test
    public void validRequestPasses() {
        VerifyResult r = verify("dev-1", now, "batch-1", "{}");
        assertTrue(r.isPassed());
    }

    @Test
    public void staleTimestampRejected() {
        long staleTs = now - 301;
        VerifyResult r = verify("dev-1", staleTs, "batch-1", "{}");
        assertFalse(r.isPassed());
        assertTrue(r.getReason().contains("timestamp"));
    }

    @Test
    public void tamperedBodyRejected() {
        String nonce = HmacSigner.newNonce();
        String sign = HmacSigner.sign(SECRET, "POST", PATH, now, nonce, "{}");
        ReportRequest req = request("dev-1", now, nonce, "batch-1", "{\"x\":1}", sign);
        assertFalse(verifier.verify(req, now).isPassed());
    }

    @Test
    public void burstOverWindowMaxRejected() {
        boolean sixthPassed = true;
        for (int i = 1; i <= 6; i++) {
            VerifyResult r = verify("dev-2", now, "batch-" + i, "{}");
            if (i == 6) {
                sixthPassed = r.isPassed();
            }
        }
        assertFalse("滑动窗口内第 6 次应被限流", sixthPassed);
    }

    @Test
    public void duplicateIdempotencyKeyRejected() {
        assertTrue(verify("dev-3", now, "batch-9", "{}").isPassed());
        // 同 deviceId + batchNo 重放，即便换新 nonce 也被拒（写侧幂等兜底）
        VerifyResult dup = verify("dev-3", now, "batch-9", "{}");
        assertFalse(dup.isPassed());
        assertTrue(dup.getReason().contains("duplicated"));
    }

    @Test
    public void differentDevicesIndependent() {
        assertTrue(verify("dev-a", now, "b-1", "{}").isPassed());
        assertTrue(verify("dev-b", now, "b-1", "{}").isPassed());
    }

    private VerifyResult verify(String deviceId, long ts, String batchNo, String body) {
        String nonce = HmacSigner.newNonce();
        String sign = HmacSigner.sign(SECRET, "POST", PATH, ts, nonce, body);
        return verifier.verify(request(deviceId, ts, nonce, batchNo, body, sign), now);
    }

    private ReportRequest request(String deviceId, long ts, String nonce,
                                  String batchNo, String body, String sign) {
        ReportRequest req = new ReportRequest();
        req.setDeviceId(deviceId);
        req.setPath(PATH);
        req.setTimestamp(ts);
        req.setNonce(nonce);
        req.setBatchNo(batchNo);
        req.setBody(body);
        req.setSign(sign);
        return req;
    }
}
