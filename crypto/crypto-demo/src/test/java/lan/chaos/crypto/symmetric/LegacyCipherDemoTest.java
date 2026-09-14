package lan.chaos.crypto.symmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyCipherDemoTest {

    private final SecureRandom random = new SecureRandom();

    @Test
    void desRoundTrip() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = LegacyCipherDemo.genDesKey();
        byte[] iv = new byte[8];
        random.nextBytes(iv);

        assertThat(key).as("DES 密钥 8 字节，有效强度仅 56 位").hasSize(8);

        byte[] ciphertext = LegacyCipherDemo.desEncrypt(key, iv, sample.toBytes());
        assertThat(LegacyCipherDemo.desDecrypt(key, iv, ciphertext)).isEqualTo(sample.toBytes());
    }

    @Test
    void tripleDesRoundTrip() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = LegacyCipherDemo.genTripleDesKey();
        byte[] iv = new byte[8];
        random.nextBytes(iv);

        assertThat(key).hasSize(24);

        byte[] ciphertext = LegacyCipherDemo.tripleDesEncrypt(key, iv, sample.toBytes());
        assertThat(LegacyCipherDemo.tripleDesDecrypt(key, iv, ciphertext)).isEqualTo(sample.toBytes());
    }

    @Test
    void rc4IsSymmetricSoEncryptAndDecryptAreTheSameOperation() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = LegacyCipherDemo.genRc4Key();

        byte[] ciphertext = LegacyCipherDemo.rc4(key, sample.toBytes());
        assertThat(ciphertext).isNotEqualTo(sample.toBytes());
        assertThat(LegacyCipherDemo.rc4(key, ciphertext)).isEqualTo(sample.toBytes());
    }
}
