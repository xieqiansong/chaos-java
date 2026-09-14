package lan.chaos.crypto.symmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChaCha20DemoTest {

    @Test
    void roundTripPreservesPlaintext() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        byte[] key = ChaCha20Demo.genKey();
        byte[] nonce = ChaCha20Demo.genNonce();

        assertThat(key).hasSize(32);
        assertThat(nonce).hasSize(12);

        byte[] ciphertext = ChaCha20Demo.encrypt(key, nonce, sample.toBytes());
        assertThat(ciphertext.length).as("密文 = 明文 + 16 字节认证标签")
                .isEqualTo(sample.toBytes().length + 16);
        assertThat(ChaCha20Demo.decrypt(key, nonce, ciphertext)).isEqualTo(sample.toBytes());
    }

    @Test
    void failsOnTamperedCiphertext() throws Exception {
        byte[] key = ChaCha20Demo.genKey();
        byte[] nonce = ChaCha20Demo.genNonce();
        byte[] ciphertext = ChaCha20Demo.encrypt(key, nonce, "hello chacha".getBytes("UTF-8"));

        ciphertext[3] ^= 0x01;
        assertThatThrownBy(() -> ChaCha20Demo.decrypt(key, nonce, ciphertext))
                .isInstanceOf(javax.crypto.AEADBadTagException.class);
    }

    @Test
    void failsOnWrongNonce() throws Exception {
        byte[] key = ChaCha20Demo.genKey();
        byte[] ciphertext = ChaCha20Demo.encrypt(key, ChaCha20Demo.genNonce(), "hello".getBytes("UTF-8"));

        assertThatThrownBy(() -> ChaCha20Demo.decrypt(key, ChaCha20Demo.genNonce(), ciphertext))
                .isInstanceOf(javax.crypto.AEADBadTagException.class);
    }
}
