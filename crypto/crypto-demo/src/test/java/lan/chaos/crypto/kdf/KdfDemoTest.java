package lan.chaos.crypto.kdf;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KdfDemoTest {

    @Test
    void pbkdf2IsDeterministicForSameSalt() throws Exception {
        byte[] salt = KdfDemo.randomSalt(16);

        String first = KdfDemo.pbkdf2Hex("P@ssw0rd", salt, 100_000, 256);
        assertThat(KdfDemo.pbkdf2Hex("P@ssw0rd", salt, 100_000, 256)).isEqualTo(first).hasSize(64);
        assertThat(KdfDemo.randomSalt(16)).as("每次生成的盐都不同").isNotEqualTo(salt);
    }

    @Test
    void saltAndIterationsBothChangeTheDerivedKey() throws Exception {
        byte[] salt = KdfDemo.randomSalt(16);
        String baseline = KdfDemo.pbkdf2Hex("P@ssw0rd", salt, 10_000, 256);

        assertThat(KdfDemo.pbkdf2Hex("P@ssw0rd", KdfDemo.randomSalt(16), 10_000, 256))
                .as("换盐 → 不同结果（彩虹表失效）").isNotEqualTo(baseline);
        assertThat(KdfDemo.pbkdf2Hex("P@ssw0rd", salt, 20_000, 256))
                .as("换迭代次数 → 不同结果").isNotEqualTo(baseline);
        assertThat(KdfDemo.pbkdf2Hex("p@ssw0rd", salt, 10_000, 256))
                .as("口令错一个字符 → 完全不同").isNotEqualTo(baseline);
    }

    @Test
    void pbkdf2OutputLengthIsConfigurable() throws Exception {
        byte[] salt = KdfDemo.randomSalt(16);
        assertThat(KdfDemo.pbkdf2("pw", salt, 1_000, 128)).hasSize(16);
        assertThat(KdfDemo.pbkdf2("pw", salt, 1_000, 256)).hasSize(32);
    }

    @Test
    void hkdfSeparatesSubKeysByInfo() {
        byte[] inputKeyMaterial = KdfDemo.randomSalt(32);
        byte[] salt = KdfDemo.randomSalt(16);

        byte[] encKey = KdfDemo.hkdf(inputKeyMaterial, salt, "aes-gcm-key".getBytes(StandardCharsets.UTF_8), 256);
        byte[] macKey = KdfDemo.hkdf(inputKeyMaterial, salt, "hmac-key".getBytes(StandardCharsets.UTF_8), 256);

        assertThat(encKey).hasSize(32);
        assertThat(encKey).isNotEqualTo(macKey).as("不同 info → 用途隔离的不同子密钥");
        assertThat(KdfDemo.hkdf(inputKeyMaterial, salt, "aes-gcm-key".getBytes(StandardCharsets.UTF_8), 256))
                .as("相同输入可复现").isEqualTo(encKey);
    }
}
