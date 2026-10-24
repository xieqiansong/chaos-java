package lan.chaos.oauth2.auth.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** PKCE S256 工具单元测试（纯函数，无需 Spring 上下文）。 */
class PkceUtilTest {

    @Test
    void challengeIsDeterministicAndVerifiable() {
        String verifier = PkceUtil.generateVerifier();
        String challenge = PkceUtil.challengeS256(verifier);
        assertThat(challenge).isNotBlank();
        assertThat(PkceUtil.verify(verifier, challenge)).isTrue();
    }

    @Test
    void wrongVerifierFails() {
        String verifier = PkceUtil.generateVerifier();
        String challenge = PkceUtil.challengeS256(verifier);
        assertThat(PkceUtil.verify("not-the-verifier", challenge)).isFalse();
    }

    @Test
    void differentVerifiersProduceDifferentChallenges() {
        String a = PkceUtil.challengeS256(PkceUtil.generateVerifier());
        String b = PkceUtil.challengeS256(PkceUtil.generateVerifier());
        assertThat(a).isNotEqualTo(b);
    }
}
