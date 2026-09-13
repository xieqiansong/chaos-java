package lan.chaos.crypto.asymmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;

class EccDemoTest {

    @Test
    void ecdsaSignAndVerify() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = EccDemo.genKeyPair(EccDemo.CURVE_P256);

        byte[] signature = EccDemo.sign(keyPair.getPrivate(), sample.toBytes());
        assertThat(EccDemo.verify(keyPair.getPublic(), sample.toBytes(), signature)).isTrue();
        assertThat(EccDemo.verify(keyPair.getPublic(), (sample.plaintext() + "x").getBytes("UTF-8"), signature)).isFalse();
    }

    @Test
    void ecdsaSignatureIsMuchShorterThanRsa() throws Exception {
        byte[] data = CryptoSample.sampleSecret().toBytes();
        KeyPair ec = EccDemo.genKeyPair(EccDemo.CURVE_P256);
        KeyPair rsa = RsaDemo.genKeyPair(3072);

        byte[] ecSignature = EccDemo.sign(ec.getPrivate(), data);
        byte[] rsaSignature = RsaDemo.sign(rsa.getPrivate(), data);

        assertThat(ecSignature.length).as("P-256 签名约 70 字节").isLessThan(80);
        assertThat(rsaSignature.length).as("RSA-3072 签名 384 字节").isEqualTo(384);
    }

    @Test
    void ecdhBothPartiesDeriveTheSameSecret() throws Exception {
        KeyPair alice = EccDemo.genKeyPair(EccDemo.CURVE_P256);
        KeyPair bob = EccDemo.genKeyPair(EccDemo.CURVE_P256);

        byte[] aliceSecret = EccDemo.agreeSecret(alice.getPrivate(), bob.getPublic());
        byte[] bobSecret = EccDemo.agreeSecret(bob.getPrivate(), alice.getPublic());

        assertThat(aliceSecret).isEqualTo(bobSecret).hasSize(32);
    }

    @Test
    void p384CurveAlsoWorks() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = EccDemo.genKeyPair(EccDemo.CURVE_P384);

        byte[] signature = EccDemo.sign(keyPair.getPrivate(), sample.toBytes());
        assertThat(EccDemo.verify(keyPair.getPublic(), sample.toBytes(), signature)).isTrue();
        assertThat(EccDemo.agreeSecret(keyPair.getPrivate(), EccDemo.genKeyPair(EccDemo.CURVE_P384).getPublic()))
                .hasSize(48);
    }
}
