package lan.chaos.crypto.practice;

import lan.chaos.crypto.asymmetric.RsaDemo;
import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridCryptoDemoTest {

    @Test
    void envelopeRoundTripPreservesPlaintext() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = RsaDemo.genKeyPair(2048);

        HybridCryptoDemo.Envelope envelope = HybridCryptoDemo.encrypt(keyPair.getPublic(), sample.toBytes());

        assertThat(envelope.wrappedKey()).as("被 RSA 包裹的 AES 会话密钥，长度 = RSA 模长").hasSize(256);
        assertThat(envelope.nonce()).hasSize(12);
        assertThat(envelope.ciphertext().length).isEqualTo(sample.toBytes().length + 16);
        assertThat(HybridCryptoDemo.decrypt(keyPair.getPrivate(), envelope)).isEqualTo(sample.toBytes());
    }

    @Test
    void sessionKeyIsFreshForEveryMessage() throws Exception {
        byte[] data = CryptoSample.sampleSecret().toBytes();
        KeyPair keyPair = RsaDemo.genKeyPair(2048);

        HybridCryptoDemo.Envelope first = HybridCryptoDemo.encrypt(keyPair.getPublic(), data);
        HybridCryptoDemo.Envelope second = HybridCryptoDemo.encrypt(keyPair.getPublic(), data);

        assertThat(first.wrappedKey()).as("每次都换会话密钥，绝不复用").isNotEqualTo(second.wrappedKey());
        assertThat(first.nonce()).isNotEqualTo(second.nonce());
        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
    }

    @Test
    void wrongPrivateKeyCannotOpenTheEnvelope() throws Exception {
        KeyPair keyPair = RsaDemo.genKeyPair(2048);
        KeyPair another = RsaDemo.genKeyPair(2048);
        HybridCryptoDemo.Envelope envelope = HybridCryptoDemo.encrypt(keyPair.getPublic(), "secret".getBytes("UTF-8"));

        assertThatThrownBy(() -> HybridCryptoDemo.decrypt(another.getPrivate(), envelope))
                .isInstanceOf(Exception.class);
    }

    @Test
    void worksForDataFarLargerThanRsaLimit() throws Exception {
        KeyPair keyPair = RsaDemo.genKeyPair(2048);
        byte[] big = new byte[512 * 1024];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) (i * 31);
        }

        HybridCryptoDemo.Envelope envelope = HybridCryptoDemo.encrypt(keyPair.getPublic(), big);

        assertThat(envelope.ciphertext().length).isEqualTo(big.length + 16);
        assertThat(Arrays.equals(big, HybridCryptoDemo.decrypt(keyPair.getPrivate(), envelope))).isTrue();
    }
}
