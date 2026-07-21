package lan.chaos.crypto.sm;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class SmCryptoDemoTest {

    @Test
    void sm4RoundTrip() throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        byte[] key = SmCryptoDemo.genSm4Key();
        byte[] iv = new byte[16];
        new java.security.SecureRandom().nextBytes(iv);

        byte[] ct = SmCryptoDemo.sm4Encrypt(key, iv, s.toBytes());
        byte[] pt = SmCryptoDemo.sm4Decrypt(key, iv, ct);
        assertArrayEquals(s.toBytes(), pt, "SM4 解密后应还原明文");
    }

    @Test
    void sm3Produces256bitHex() throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        String h = SmCryptoDemo.sm3Hex(s.toBytes());
        assertEquals(64, h.length(), "SM3 十六进制应为 64 字符");
        assertEquals(h, SmCryptoDemo.sm3Hex(s.toBytes()), "相同输入 SM3 应一致");
    }

    @Test
    void sm2SignAndVerify() throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        KeyPair kp = SmCryptoDemo.genSm2KeyPair();

        byte[] sig = SmCryptoDemo.sm2Sign(kp.getPrivate(), s.toBytes());
        assertTrue(SmCryptoDemo.sm2Verify(kp.getPublic(), s.toBytes(), sig), "SM2 合法签名应验签通过");

        byte[] tampered = (s.plaintext() + "x").getBytes();
        assertFalse(SmCryptoDemo.sm2Verify(kp.getPublic(), tampered, sig), "篡改后 SM2 验签应失败");
    }
}
