package lan.chaos.hmac;

import lan.chaos.hmac.core.HmacSigner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 签名 / 验签正确性。 */
public class HmacSignerTest {

    private static final String SECRET = "test-secret";
    private static final long TS = 1_700_000_000L;

    @Test
    public void correctSecretSignVerifyPasses() {
        String nonce = HmacSigner.newNonce();
        String body = "{\"online\":1}";
        String sign = HmacSigner.sign(SECRET, "POST", "/v1/report", TS, nonce, body);
        assertTrue(HmacSigner.verify(SECRET, "POST", "/v1/report", TS, nonce, body, sign));
    }

    @Test
    public void tamperedBodyFails() {
        String nonce = HmacSigner.newNonce();
        String body = "{\"online\":1,\"cpu\":42}";
        String sign = HmacSigner.sign(SECRET, "POST", "/v1/report", TS, nonce, body);
        assertFalse(HmacSigner.verify(SECRET, "POST", "/v1/report", TS, nonce,
                "{\"online\":1,\"cpu\":99}", sign));
    }

    @Test
    public void wrongSecretFails() {
        String nonce = HmacSigner.newNonce();
        String body = "{}";
        String sign = HmacSigner.sign("other-secret", "POST", "/v1/report", TS, nonce, body);
        assertFalse(HmacSigner.verify(SECRET, "POST", "/v1/report", TS, nonce, body, sign));
    }

    @Test
    public void tamperedTimestampFails() {
        String nonce = HmacSigner.newNonce();
        String body = "{}";
        String sign = HmacSigner.sign(SECRET, "POST", "/v1/report", TS, nonce, body);
        assertFalse(HmacSigner.verify(SECRET, "POST", "/v1/report", TS + 1, nonce, body, sign));
    }

    @Test
    public void sameInputSameSignDeterministic() {
        String nonce = "aabbccddeeff00112233445566778899";
        String body = "{}";
        String s1 = HmacSigner.sign(SECRET, "POST", "/v1/report", TS, nonce, body);
        String s2 = HmacSigner.sign(SECRET, "POST", "/v1/report", TS, nonce, body);
        assertTrue(s1.equals(s2));
    }

    @Test
    public void differentNonceDifferentSign() {
        String body = "{}";
        String s1 = HmacSigner.sign(SECRET, "POST", "/v1/report", TS, "nonce-1", body);
        String s2 = HmacSigner.sign(SECRET, "POST", "/v1/report", TS, "nonce-2", body);
        assertNotEquals(s1, s2);
    }

    @Test
    public void emptyOrNullSignFails() {
        assertFalse(HmacSigner.verify(SECRET, "POST", "/v1/report", TS, "n", "{}", null));
        assertFalse(HmacSigner.verify(SECRET, "POST", "/v1/report", TS, "n", "{}", ""));
    }
}
