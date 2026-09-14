package lan.chaos.crypto.digest;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MacDemoTest {

    private static final byte[] KEY = "shared-secret-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    @Test
    void hmacSha256KnownAnswerTest() throws Exception {
        byte[] key = "key".getBytes(StandardCharsets.UTF_8);
        byte[] message = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

        assertThat(MacDemo.mac(MacDemo.HMAC_SHA256, key, message))
                .isEqualTo(hex("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8"));
    }

    @Test
    void macDependsOnKeyAndMessage() throws Exception {
        byte[] data = "amount=100&user=Alice".getBytes(StandardCharsets.UTF_8);
        String expected = MacDemo.hmacSha256Hex(KEY, data);

        assertThat(MacDemo.hmacSha256Hex(KEY, data)).as("同一密钥同一消息，结果稳定").isEqualTo(expected);
        assertThat(MacDemo.hmacSha256Hex("another-key-9876543210fedcba".getBytes(StandardCharsets.UTF_8), data))
                .as("换密钥结果完全不同").isNotEqualTo(expected);
        assertThat(MacDemo.hmacSha256Hex(KEY, "amount=100&user=Bob".getBytes(StandardCharsets.UTF_8)))
                .as("消息被改则 MAC 变").isNotEqualTo(expected);
    }

    @Test
    void hmacAndCmacProduceDifferentTags() throws Exception {
        byte[] data = "payload".getBytes(StandardCharsets.UTF_8);
        byte[] aesKey = new byte[16];
        System.arraycopy(KEY, 0, aesKey, 0, 16);

        assertThat(MacDemo.hmacSha256Hex(KEY, data)).hasSize(64);
        assertThat(MacDemo.cmacAesHex(aesKey, data)).hasSize(32);
        assertThat(MacDemo.cmacAesHex(aesKey, data)).isEqualTo(MacDemo.cmacAesHex(aesKey, data));
        assertThat(MacDemo.cmacAesHex(aesKey, "payload2".getBytes(StandardCharsets.UTF_8)))
                .isNotEqualTo(MacDemo.cmacAesHex(aesKey, data));
    }

    @Test
    void constantTimeEqualsBehavesLikeEquality() throws Exception {
        byte[] tag = MacDemo.mac(MacDemo.HMAC_SHA256, KEY, "x".getBytes(StandardCharsets.UTF_8));
        byte[] other = MacDemo.mac(MacDemo.HMAC_SHA256, KEY, "y".getBytes(StandardCharsets.UTF_8));

        assertThat(MacDemo.constantTimeEquals(tag, tag)).isTrue();
        assertThat(MacDemo.constantTimeEquals(tag, other)).isFalse();
    }

    private static byte[] hex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
