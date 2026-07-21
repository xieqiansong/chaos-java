package lan.chaos.crypto.rsa;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class RsaDemoTest {

    @Test
    void encryptDecryptRoundTrip() throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        KeyPair kp = RsaDemo.genKeyPair(2048);

        byte[] ct = RsaDemo.encrypt(kp.getPublic(), s.toBytes());
        byte[] pt = RsaDemo.decrypt(kp.getPrivate(), ct);
        assertArrayEquals(s.toBytes(), pt, "RSA 解密后应还原明文");
    }

    @Test
    void signAndVerify() throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        KeyPair kp = RsaDemo.genKeyPair(2048);

        byte[] sig = RsaDemo.sign(kp.getPrivate(), s.toBytes());
        assertTrue(RsaDemo.verify(kp.getPublic(), s.toBytes(), sig), "合法签名应验签通过");

        // 篡改原文后验签应失败
        byte[] tampered = (s.plaintext() + "x").getBytes();
        assertFalse(RsaDemo.verify(kp.getPublic(), tampered, sig), "篡改后验签应失败");
    }
}
