package lan.chaos.crypto.asymmetric;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;

class DhDemoTest {

    @Test
    void bothPartiesAgreeOnTheSameSecret() throws Exception {
        KeyPair alice = DhDemo.genKeyPair(2048);
        KeyPair bob = DhDemo.genKeyPair(2048);

        byte[] aliceSecret = DhDemo.agreeSecret(alice.getPrivate(), bob.getPublic());
        byte[] bobSecret = DhDemo.agreeSecret(bob.getPrivate(), alice.getPublic());

        assertThat(aliceSecret).isEqualTo(bobSecret).isNotEmpty();
    }

    @Test
    void thirdPartyGetsADifferentSecret() throws Exception {
        KeyPair alice = DhDemo.genKeyPair(2048);
        KeyPair bob = DhDemo.genKeyPair(2048);
        KeyPair eve = DhDemo.genKeyPair(2048);

        byte[] aliceBob = DhDemo.agreeSecret(alice.getPrivate(), bob.getPublic());
        byte[] aliceEve = DhDemo.agreeSecret(alice.getPrivate(), eve.getPublic());

        assertThat(aliceBob).isNotEqualTo(aliceEve).as("换成第三方公钥，协商结果完全不同");
    }
}
