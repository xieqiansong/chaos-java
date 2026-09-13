package lan.chaos.crypto.asymmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaDemoTest {

    @Test
    void oaepEncryptDecryptRoundTrip() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = RsaDemo.genKeyPair(2048);

        byte[] ciphertext = RsaDemo.encryptOaep(keyPair.getPublic(), sample.toBytes());
        assertThat(RsaDemo.decryptOaep(keyPair.getPrivate(), ciphertext)).isEqualTo(sample.toBytes());
    }

    @Test
    void pkcs1EncryptDecryptRoundTrip() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = RsaDemo.genKeyPair(2048);

        byte[] ciphertext = RsaDemo.encryptPkcs1(keyPair.getPublic(), sample.toBytes());
        assertThat(RsaDemo.decryptPkcs1(keyPair.getPrivate(), ciphertext)).isEqualTo(sample.toBytes());
    }

    @Test
    void encryptRejectsPayloadLongerThanLimit() {
        int limit = RsaDemo.oaepMaxPlaintextBytes(2048);
        assertThat(limit).as("2048 位 + OAEP-SHA256 的上限").isEqualTo(190);

        assertThatThrownBy(() -> RsaDemo.encryptOaep(RsaDemo.genKeyPair(2048).getPublic(), new byte[limit + 1]))
                .isInstanceOf(javax.crypto.IllegalBlockSizeException.class);
    }

    @Test
    void pkcs1SignatureSignAndVerify() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = RsaDemo.genKeyPair(2048);

        byte[] signature = RsaDemo.sign(keyPair.getPrivate(), sample.toBytes());
        assertThat(RsaDemo.verify(keyPair.getPublic(), sample.toBytes(), signature)).isTrue();
        assertThat(RsaDemo.verify(keyPair.getPublic(), (sample.plaintext() + "x").getBytes("UTF-8"), signature)).isFalse();
    }

    @Test
    void pssSignatureSignAndVerify() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = RsaDemo.genKeyPair(2048);

        byte[] signature = RsaDemo.signPss(keyPair.getPrivate(), sample.toBytes());
        assertThat(RsaDemo.verifyPss(keyPair.getPublic(), sample.toBytes(), signature)).isTrue();
        assertThat(RsaDemo.verifyPss(keyPair.getPublic(), (sample.plaintext() + "x").getBytes("UTF-8"), signature)).isFalse();

        // PSS 是概率签名：同一数据两次签名结果不同（PKCS1 v1.5 则相同）
        byte[] again = RsaDemo.signPss(keyPair.getPrivate(), sample.toBytes());
        assertThat(again).isNotEqualTo(signature);
        assertThat(RsaDemo.sign(keyPair.getPrivate(), sample.toBytes()))
                .as("PKCS#1 v1.5 是确定性的")
                .isEqualTo(RsaDemo.sign(keyPair.getPrivate(), sample.toBytes()));
    }

    @Test
    void verifyFailsWithAnotherKeyPair() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair signer = RsaDemo.genKeyPair(2048);
        KeyPair other = RsaDemo.genKeyPair(2048);

        byte[] signature = RsaDemo.sign(signer.getPrivate(), sample.toBytes());
        assertThat(RsaDemo.verify(other.getPublic(), sample.toBytes(), signature)).isFalse();
    }
}
