package lan.chaos.crypto.aes;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AesDemoTest {

    @Test
    void cbcRoundTripPreservesPlaintext() throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        byte[] key = AesDemo.genKey(128);
        byte[] iv = AesDemo.genIv(16);

        byte[] ct = AesDemo.encryptCbc(key, iv, s.toBytes());
        assertNotEquals(s.plaintext(), new String(ct, StandardCharsets.UTF_8), "密文不应等于明文");

        byte[] pt = AesDemo.decryptCbc(key, iv, ct);
        assertArrayEquals(s.toBytes(), pt, "CBC 解密后应还原明文");
    }

    @Test
    void gcmRoundTripPreservesPlaintextAndIsAuthenticated() throws Exception {
        CryptoSample s = CryptoSample.sampleSecret();
        byte[] key = AesDemo.genKey(128);
        byte[] iv = AesDemo.genIv(12);

        byte[] ct = AesDemo.encryptGcm(key, iv, s.toBytes());
        byte[] pt = AesDemo.decryptGcm(key, iv, ct);
        assertArrayEquals(s.toBytes(), pt, "GCM 解密后应还原明文");
    }

    @Test
    void gcmFailsOnTamperedCiphertext() throws Exception {
        byte[] key = AesDemo.genKey(128);
        byte[] iv = AesDemo.genIv(12);
        byte[] ct = AesDemo.encryptGcm(key, iv, "hello".getBytes());

        // 篡改密文最后一字节：GCM 认证应拒绝（抛 AEADBadTagException）
        ct[ct.length - 1] ^= 0x01;
        assertThrows(javax.crypto.AEADBadTagException.class,
                () -> AesDemo.decryptGcm(key, iv, ct), "GCM 应检测到密文被篡改");
    }
}
