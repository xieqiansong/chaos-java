package lan.chaos.crypto.sm;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmCryptoDemoTest {

    private final SecureRandom random = new SecureRandom();

    @Test
    void sm4CbcRoundTrip() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = SmCryptoDemo.genSm4Key();
        byte[] iv = new byte[16];
        random.nextBytes(iv);

        assertThat(key).hasSize(16);
        byte[] ciphertext = SmCryptoDemo.sm4EncryptCbc(key, iv, sample.toBytes());
        assertThat(SmCryptoDemo.sm4DecryptCbc(key, iv, ciphertext)).isEqualTo(sample.toBytes());
    }

    @Test
    void sm4GcmRoundTripAndTamperDetection() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = SmCryptoDemo.genSm4Key();
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);

        byte[] ciphertext = SmCryptoDemo.sm4EncryptGcm(key, nonce, sample.toBytes());
        assertThat(SmCryptoDemo.sm4DecryptGcm(key, nonce, ciphertext)).isEqualTo(sample.toBytes());

        ciphertext[ciphertext.length - 1] ^= 0x01;
        assertThatThrownBy(() -> SmCryptoDemo.sm4DecryptGcm(key, nonce, ciphertext))
                .isInstanceOf(javax.crypto.AEADBadTagException.class);
    }

    @Test
    void sm3KnownAnswerAndLength() throws Exception {
        // GB/T 32905-2016 标准向量：SM3("abc")
        assertThat(SmCryptoDemo.sm3Hex("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");

        String hash = SmCryptoDemo.sm3Hex(CryptoSample.sampleSecret().toBytes());
        assertThat(hash).hasSize(64);
        assertThat(SmCryptoDemo.sm3Hex(CryptoSample.sampleSecret().toBytes())).isEqualTo(hash);
    }

    @Test
    void sm2SignAndVerify() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = SmCryptoDemo.genSm2KeyPair();

        byte[] signature = SmCryptoDemo.sm2Sign(keyPair.getPrivate(), sample.toBytes());
        assertThat(SmCryptoDemo.sm2Verify(keyPair.getPublic(), sample.toBytes(), signature)).isTrue();
        assertThat(SmCryptoDemo.sm2Verify(keyPair.getPublic(), (sample.plaintext() + "x").getBytes(StandardCharsets.UTF_8), signature))
                .isFalse();
    }

    @Test
    void sm2EncryptAndDecrypt() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = SmCryptoDemo.genSm2KeyPair();

        byte[] ciphertext = SmCryptoDemo.sm2Encrypt(keyPair.getPublic(), sample.toBytes());
        assertThat(ciphertext).isNotEqualTo(sample.toBytes());
        assertThat(SmCryptoDemo.sm2Decrypt(keyPair.getPrivate(), ciphertext)).isEqualTo(sample.toBytes());
    }
}
