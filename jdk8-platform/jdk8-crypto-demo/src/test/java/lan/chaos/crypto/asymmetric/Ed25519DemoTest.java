package lan.chaos.crypto.asymmetric;

import lan.chaos.crypto.common.model.CryptoSample;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;

class Ed25519DemoTest {

    @Test
    void ed25519SignAndVerify() throws Exception {
        CryptoSample sample = CryptoSample.sampleSecret();
        KeyPair keyPair = Ed25519Demo.genEd25519KeyPair();

        byte[] signature = Ed25519Demo.sign(keyPair.getPrivate(), sample.toBytes());
        assertThat(signature).as("Ed25519 签名固定 64 字节").hasSize(64);
        assertThat(Ed25519Demo.verify(keyPair.getPublic(), sample.toBytes(), signature)).isTrue();
        assertThat(Ed25519Demo.verify(keyPair.getPublic(), (sample.plaintext() + "x").getBytes("UTF-8"), signature)).isFalse();
    }

    @Test
    void ed25519SignatureIsDeterministic() throws Exception {
        byte[] data = CryptoSample.sampleSecret().toBytes();
        KeyPair keyPair = Ed25519Demo.genEd25519KeyPair();

        assertThat(Ed25519Demo.sign(keyPair.getPrivate(), data))
                .as("EdDSA 不依赖随机数，同数据同密钥签名必然相同 —— 这正是它比 ECDSA 更难用错的地方")
                .isEqualTo(Ed25519Demo.sign(keyPair.getPrivate(), data));
    }

    @Test
    void x25519BothPartiesDeriveTheSameSecret() throws Exception {
        KeyPair alice = Ed25519Demo.genX25519KeyPair();
        KeyPair bob = Ed25519Demo.genX25519KeyPair();

        byte[] aliceSecret = Ed25519Demo.agreeSecret(alice.getPrivate(), bob.getPublic());
        byte[] bobSecret = Ed25519Demo.agreeSecret(bob.getPrivate(), alice.getPublic());

        assertThat(aliceSecret).isEqualTo(bobSecret).hasSize(32);
    }
}
