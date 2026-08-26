package lan.chaos.hmac;

import lan.chaos.hmac.core.HmacSigner;
import lan.chaos.hmac.core.SecretKeyStore;
import lan.chaos.hmac.model.ReportRequest;
import lan.chaos.hmac.rotate.KeyRotationManager;
import lan.chaos.hmac.verify.ReplayGuard;
import lan.chaos.hmac.verify.RequestVerifier;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 双密钥过渡轮换：新钥生效 / 宽限期旧钥可验 / 宽限期后旧钥拒绝。 */
public class KeyRotationManagerTest {

    private static final String SECRET_V1 = "secret-v1";
    private static final String SECRET_V2 = "secret-v2";
    private static final String PATH = "/v1/report";
    private static final String METHOD = "POST";

    private long now;
    private SecretKeyStore keyStore;
    private KeyRotationManager rotation;
    private RequestVerifier verifier;

    @Before
    public void setUp() {
        this.now = System.currentTimeMillis() / 1000L;
        this.keyStore = new SecretKeyStore(SECRET_V1);
        this.rotation = new KeyRotationManager(keyStore);
        this.verifier = new RequestVerifier(keyStore, new ReplayGuard(100, 60_000L), 300);
    }

    @Test
    public void rotateThenNewKeySignImmediatelyValid() {
        rotation.rotate(SECRET_V2);
        assertTrue(signVerify(SECRET_V2, "dev-new", "batch-new"));
    }

    @Test
    public void oldKeyStillValidDuringGracePeriod() {
        rotation.rotate(SECRET_V2);
        assertTrue("宽限期内旧密钥仍可验签（在途设备未升级）", signVerify(SECRET_V1, "dev-old", "batch-old"));
    }

    @Test
    public void completeRotationThenOldKeyRejected() {
        rotation.rotate(SECRET_V2);
        rotation.completeRotation();
        assertFalse("宽限期结束后旧密钥应被拒绝", signVerify(SECRET_V1, "dev-old", "batch-old"));
        assertTrue("新密钥不受影响", signVerify(SECRET_V2, "dev-new", "batch-new"));
    }

    @Test
    public void multiStageRolloverChain() {
        // 连续两次轮换：v1 -> v2 -> v3，宽限期只保留最近一版旧钥
        rotation.rotate(SECRET_V2);
        rotation.rotate("secret-v3");
        assertTrue("v2 宽限期内可验", signVerify(SECRET_V2, "dev-2", "batch-2"));
        assertFalse("v1 已被丢弃", signVerify(SECRET_V1, "dev-1", "batch-1"));
        assertTrue("v3 立即生效", signVerify("secret-v3", "dev-3", "batch-3"));
    }

    private boolean signVerify(String secret, String deviceId, String batchNo) {
        long ts = now;
        String nonce = HmacSigner.newNonce();
        String body = "{}";
        String sign = HmacSigner.sign(secret, METHOD, PATH, ts, nonce, body);
        ReportRequest req = new ReportRequest();
        req.setDeviceId(deviceId);
        req.setPath(PATH);
        req.setTimestamp(ts);
        req.setNonce(nonce);
        req.setBatchNo(batchNo);
        req.setBody(body);
        req.setSign(sign);
        return verifier.verify(req, now).isPassed();
    }
}
